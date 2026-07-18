package com.casamento.backend.service;

import com.casamento.backend.config.SiteContext;
import com.casamento.backend.dto.CompraCarrinhoRequest;
import com.casamento.backend.dto.FinalizarCarrinhoResponse;
import com.casamento.backend.dto.GerarPixResponse;
import com.casamento.backend.model.HistoricoCompraCota;
import com.casamento.backend.model.PedidoPresente;
import com.casamento.backend.model.PresenteCasamento;
import com.casamento.backend.model.Site;
import com.casamento.backend.repository.HistoricoCompraRepository;
import com.casamento.backend.repository.PedidoPresenteRepository;
import com.casamento.backend.repository.PresenteRepository;
import com.casamento.backend.repository.SiteRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PresenteService {

    private final PresenteRepository presenteRepository;
    private final HistoricoCompraRepository historicoCompraRepository;
    private final PedidoPresenteRepository pedidoPresenteRepository;
    private final SiteRepository siteRepository;
    private final PixPayloadService pixPayloadService;
    private final MercadoPagoService mercadoPagoService;
    private final ObjectMapper objectMapper;
    private final String sitePublicBaseUrl;

    public PresenteService(
            PresenteRepository presenteRepository,
            HistoricoCompraRepository historicoCompraRepository,
            PedidoPresenteRepository pedidoPresenteRepository,
            SiteRepository siteRepository,
            PixPayloadService pixPayloadService,
            MercadoPagoService mercadoPagoService,
            ObjectMapper objectMapper,
            @Value("${app.site-public-base-url:https://rafaekevin.com.br}") String sitePublicBaseUrl) {
        this.presenteRepository = presenteRepository;
        this.historicoCompraRepository = historicoCompraRepository;
        this.pedidoPresenteRepository = pedidoPresenteRepository;
        this.siteRepository = siteRepository;
        this.pixPayloadService = pixPayloadService;
        this.mercadoPagoService = mercadoPagoService;
        this.objectMapper = objectMapper;
        this.sitePublicBaseUrl = sitePublicBaseUrl == null || sitePublicBaseUrl.isBlank()
                ? "https://rafaekevin.com.br"
                : sitePublicBaseUrl.replaceAll("/$", "");
    }

    public GerarPixResponse gerarPix(CompraCarrinhoRequest request) {
        exigirSiteAtual();
        BigDecimal total = calcularTotalSemReservar(request.getItens());
        String txid = pixPayloadService.gerarTxid();
        String pixCopiaCola = pixPayloadService.gerarPayload(total, txid);

        return new GerarPixResponse(
                total,
                pixCopiaCola,
                txid,
                "Escaneie o QR Code ou copie o código PIX para pagar."
        );
    }

    /**
     * Convidado avisou que pagou PIX. Não baixa cotas — cria pedido AGUARDANDO_PIX
     * para a noiva confirmar no painel (após ver o crédito na conta).
     */
    @Transactional
    public FinalizarCarrinhoResponse finalizarCarrinho(CompraCarrinhoRequest request) {
        Site site = exigirSiteAtual();

        if (request.getNomeComprador() == null || request.getNomeComprador().isBlank()) {
            throw new IllegalArgumentException("Informe o nome do comprador.");
        }

        Map<Long, Integer> quantidadePorPresente = agruparItens(request.getItens());
        BigDecimal total = calcularTotal(quantidadePorPresente);

        PedidoPresente pedido = new PedidoPresente();
        pedido.setSite(site);
        pedido.setNomeComprador(request.getNomeComprador().trim());
        pedido.setTotal(total);
        pedido.setStatus("AGUARDANDO_PIX");
        try {
            pedido.setItensJson(objectMapper.writeValueAsString(request.getItens()));
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível registrar o aviso de pagamento.");
        }
        pedidoPresenteRepository.save(pedido);

        return new FinalizarCarrinhoResponse(
                total,
                "Obrigado! Recebemos seu aviso. O casal confirma o presente após o PIX aparecer na conta — as cotas só baixam nessa confirmação."
        );
    }

    @Transactional
    public FinalizarCarrinhoResponse confirmarPixManual(Long pedidoId) {
        Site site = exigirSiteAtual();
        PedidoPresente pedido = pedidoPresenteRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado."));
        if (pedido.getSite() == null || !site.getId().equals(pedido.getSite().getId())) {
            throw new IllegalArgumentException("Pedido não pertence a este casamento.");
        }
        if ("PAGO".equalsIgnoreCase(pedido.getStatus())) {
            return new FinalizarCarrinhoResponse(pedido.getTotal(), "Presente já estava confirmado.");
        }
        if (!"AGUARDANDO_PIX".equalsIgnoreCase(pedido.getStatus())) {
            throw new IllegalStateException("Este pedido não está aguardando confirmação de PIX.");
        }
        confirmarPedidoPago(pedido, "pix-manual:" + pedido.getId());
        return new FinalizarCarrinhoResponse(
                pedido.getTotal(),
                "PIX confirmado! Cotas reservadas com sucesso."
        );
    }

    @Transactional
    public void recusarPixManual(Long pedidoId) {
        Site site = exigirSiteAtual();
        PedidoPresente pedido = pedidoPresenteRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado."));
        if (pedido.getSite() == null || !site.getId().equals(pedido.getSite().getId())) {
            throw new IllegalArgumentException("Pedido não pertence a este casamento.");
        }
        if (!"AGUARDANDO_PIX".equalsIgnoreCase(pedido.getStatus())) {
            throw new IllegalStateException("Só é possível recusar avisos de PIX pendentes.");
        }
        pedido.setStatus("CANCELADO");
        pedidoPresenteRepository.save(pedido);
    }

    @Transactional
    public Map<String, Object> iniciarCheckoutCartao(CompraCarrinhoRequest request) {
        Site site = exigirSiteAtual();
        if (!site.isMpSellerConectado()) {
            throw new IllegalStateException(
                    "Cartão indisponível neste momento. Use PIX ou peça à noiva para conectar o Mercado Pago.");
        }
        if (request.getNomeComprador() == null || request.getNomeComprador().isBlank()) {
            throw new IllegalArgumentException("Informe o nome do comprador.");
        }

        Map<Long, Integer> qtd = agruparItens(request.getItens());
        BigDecimal total = calcularTotal(qtd);

        PedidoPresente pedido = new PedidoPresente();
        pedido.setSite(site);
        pedido.setNomeComprador(request.getNomeComprador().trim());
        pedido.setTotal(total);
        pedido.setStatus("PENDENTE");
        try {
            pedido.setItensJson(objectMapper.writeValueAsString(request.getItens()));
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível preparar o pedido.");
        }
        pedido = pedidoPresenteRepository.save(pedido);

        String base = sitePublicBaseUrl + "/index.html?site=" + MercadoPagoService.encodeQuery(site.getSlug());
        String ok = base + "&presente_pago=1&pedido=" + pedido.getId();
        String fail = base + "&presente_pago=0";

        Map<String, String> pref = mercadoPagoService.criarPreferenciaPresente(
                site.getMpSellerAccessToken(),
                pedido.getId(),
                "Presente — " + site.getNomeNoiva() + " & " + site.getNomeNoivo(),
                total,
                ok,
                fail
        );
        pedido.setMpPreferenceId(pref.get("id"));
        pedidoPresenteRepository.save(pedido);

        Map<String, Object> out = new HashMap<>();
        out.put("pedidoId", pedido.getId());
        out.put("checkoutUrl", pref.get("init_point"));
        out.put("total", total);
        out.put("mensagem", "Você será redirecionado ao Mercado Pago. O valor vai direto para o casal.");
        return out;
    }

    @Transactional
    public void processarPagamentoPresente(String paymentId, String userIdHint) {
        if (paymentId == null || paymentId.isBlank()) return;

        // Já processado?
        if (pedidoPresenteRepository.findByMpPaymentId(paymentId).isPresent()) {
            return;
        }

        Site siteHint = null;
        if (userIdHint != null && !userIdHint.isBlank()) {
            siteHint = siteRepository.findByMpSellerUserId(userIdHint.trim()).orElse(null);
        }

        JsonNode pagamento = null;
        PedidoPresente pedido = null;

        if (siteHint != null && siteHint.isMpSellerConectado()) {
            try {
                pagamento = mercadoPagoService.buscarPagamentoComToken(paymentId, siteHint.getMpSellerAccessToken());
                pedido = resolverPedido(pagamento);
            } catch (Exception ignored) {
            }
        }

        if (pagamento == null) {
            // tenta token da plataforma (às vezes funciona) e depois localiza pedido
            try {
                pagamento = mercadoPagoService.buscarPagamento(paymentId);
                pedido = resolverPedido(pagamento);
                if (pedido != null && pedido.getSite() != null && pedido.getSite().isMpSellerConectado()) {
                    // revalida com token da noiva
                    pagamento = mercadoPagoService.buscarPagamentoComToken(
                            paymentId, pedido.getSite().getMpSellerAccessToken());
                }
            } catch (Exception ignored) {
            }
        }

        if (pedido == null && pagamento != null) {
            pedido = resolverPedido(pagamento);
        }

        // Último recurso: se só temos userId, não achamos external — aborta
        if (pedido == null || pagamento == null) {
            return;
        }

        String status = pagamento.path("status").asText("");
        if (!"approved".equalsIgnoreCase(status)) {
            return;
        }
        confirmarPedidoPago(pedido, paymentId);
    }

    @Transactional
    public FinalizarCarrinhoResponse confirmarPagamentoRetorno(Long pedidoId, String paymentId) {
        Site site = exigirSiteAtual();
        PedidoPresente pedido = pedidoPresenteRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado."));
        if (pedido.getSite() == null || !site.getId().equals(pedido.getSite().getId())) {
            throw new IllegalArgumentException("Pedido não pertence a este casamento.");
        }
        if ("PAGO".equalsIgnoreCase(pedido.getStatus())) {
            return new FinalizarCarrinhoResponse(pedido.getTotal(), "Presente já confirmado. Obrigado!");
        }
        if (!site.isMpSellerConectado()) {
            throw new IllegalStateException("Mercado Pago não conectado.");
        }

        JsonNode pagamento = mercadoPagoService.buscarPagamentoComToken(paymentId, site.getMpSellerAccessToken());
        String status = pagamento.path("status").asText("");
        String external = pagamento.path("external_reference").asText("");
        if (!external.equals("pedido:" + pedidoId)) {
            throw new IllegalArgumentException("Pagamento não corresponde a este pedido.");
        }
        if (!"approved".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Pagamento ainda não aprovado (" + status + ").");
        }
        confirmarPedidoPago(pedido, paymentId);
        return new FinalizarCarrinhoResponse(
                pedido.getTotal(),
                "Pagamento aprovado! Obrigado pelo carinho."
        );
    }

    private PedidoPresente resolverPedido(JsonNode pagamento) {
        if (pagamento == null) return null;
        String external = pagamento.path("external_reference").asText("");
        if (external.startsWith("pedido:")) {
            try {
                Long id = Long.parseLong(external.substring("pedido:".length()).trim());
                return pedidoPresenteRepository.findById(id).orElse(null);
            } catch (NumberFormatException ignored) {
            }
        }
        String pref = pagamento.path("preference_id").asText("");
        if (!pref.isBlank()) {
            return pedidoPresenteRepository.findByMpPreferenceId(pref).orElse(null);
        }
        return null;
    }

    private void confirmarPedidoPago(PedidoPresente pedido, String paymentId) {
        if ("PAGO".equalsIgnoreCase(pedido.getStatus())) {
            return;
        }
        Site siteAnterior = SiteContext.get();
        try {
            SiteContext.set(pedido.getSite());
            List<CompraCarrinhoRequest.ItemCarrinho> itens = objectMapper.readValue(
                    pedido.getItensJson(),
                    new TypeReference<List<CompraCarrinhoRequest.ItemCarrinho>>() {}
            );
            Map<Long, Integer> qtd = agruparItens(itens);
            reservarCotas(qtd, pedido.getNomeComprador());
            pedido.setStatus("PAGO");
            pedido.setMpPaymentId(paymentId);
            pedido.setDataPagamento(LocalDateTime.now());
            pedidoPresenteRepository.save(pedido);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao confirmar cotas do pedido " + pedido.getId() + ": " + e.getMessage(), e);
        } finally {
            SiteContext.clear();
            if (siteAnterior != null) {
                SiteContext.set(siteAnterior);
            }
        }
    }

    public void conectarSeller(Site site, String accessToken, String refreshToken, String userId) {
        site.setMpSellerAccessToken(accessToken);
        site.setMpSellerRefreshToken(blankToNull(refreshToken));
        site.setMpSellerUserId(blankToNull(userId));
        site.setMpSellerConectadoEm(Instant.now());
        siteRepository.save(site);
    }

    public void desconectarSeller(Site site) {
        site.setMpSellerAccessToken(null);
        site.setMpSellerRefreshToken(null);
        site.setMpSellerUserId(null);
        site.setMpSellerConectadoEm(null);
        siteRepository.save(site);
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private Site exigirSiteAtual() {
        Site site = SiteContext.get();
        if (site == null) {
            throw new IllegalArgumentException(
                    "Informe o header X-Site-Id com o slug do casamento (ex: rafaekevin)."
            );
        }
        return site;
    }

    private PresenteCasamento buscarPresenteDoSite(Long id) {
        Site site = exigirSiteAtual();
        PresenteCasamento presente = presenteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Presente não encontrado."));

        if (presente.getSite() == null || !site.getId().equals(presente.getSite().getId())) {
            throw new IllegalArgumentException("Presente não pertence a este casamento.");
        }
        return presente;
    }

    private Map<Long, Integer> agruparItens(List<CompraCarrinhoRequest.ItemCarrinho> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("Seu carrinho está vazio.");
        }

        Map<Long, Integer> quantidadePorPresente = new HashMap<>();

        for (CompraCarrinhoRequest.ItemCarrinho item : itens) {
            if (item.getPresenteId() == null) {
                throw new IllegalArgumentException("Item do carrinho inválido.");
            }
            if (item.getQuantidade() == null || item.getQuantidade() < 1) {
                throw new IllegalArgumentException("Quantidade inválida para um dos itens.");
            }
            quantidadePorPresente.merge(item.getPresenteId(), item.getQuantidade(), Integer::sum);
        }

        return quantidadePorPresente;
    }

    private BigDecimal calcularTotalSemReservar(List<CompraCarrinhoRequest.ItemCarrinho> itens) {
        return calcularTotal(agruparItens(itens));
    }

    private BigDecimal calcularTotal(Map<Long, Integer> quantidadePorPresente) {
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entrada : quantidadePorPresente.entrySet()) {
            PresenteCasamento presente = buscarPresenteDoSite(entrada.getKey());

            int quantidade = entrada.getValue();
            int disponiveis = presente.getCotasDisponiveis();

            if (quantidade > disponiveis) {
                throw new IllegalArgumentException(
                        "Não há cotas suficientes para \"" + presente.getNome() + "\". Disponível: " + disponiveis
                );
            }

            total = total.add(presente.getValor().multiply(BigDecimal.valueOf(quantidade)));
        }

        return total;
    }

    private BigDecimal reservarCotas(Map<Long, Integer> quantidadePorPresente, String nomeComprador) {
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entrada : quantidadePorPresente.entrySet()) {
            PresenteCasamento presente = buscarPresenteDoSite(entrada.getKey());

            int quantidade = entrada.getValue();
            int disponiveis = presente.getCotasDisponiveis();

            if (quantidade > disponiveis) {
                throw new IllegalArgumentException(
                        "Não há cotas suficientes para \"" + presente.getNome() + "\". Disponível: " + disponiveis
                );
            }

            presente.setCotasVendidas(presente.getCotasVendidas() + quantidade);
            presente.atualizarStatusComprado();
            presente.setNomeComprador(nomeComprador);
            presenteRepository.save(presente);
            registrarHistorico(presente, nomeComprador, quantidade);

            total = total.add(presente.getValor().multiply(BigDecimal.valueOf(quantidade)));
        }

        return total;
    }

    private void registrarHistorico(PresenteCasamento presente, String nomeComprador, int quantidade) {
        HistoricoCompraCota historico = new HistoricoCompraCota();
        historico.setPresenteId(presente.getId());
        historico.setNomePresente(presente.getNome());
        historico.setNomeComprador(nomeComprador);
        historico.setQuantidade(quantidade);
        historico.setValorCota(presente.getValor());
        historico.setValorTotal(presente.getValor().multiply(BigDecimal.valueOf(quantidade)));
        historicoCompraRepository.save(historico);
    }
}
