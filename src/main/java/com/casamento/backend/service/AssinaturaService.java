package com.casamento.backend.service;

import com.casamento.backend.model.Site;
import com.casamento.backend.model.UsuarioNoiva;
import com.casamento.backend.repository.SiteRepository;
import com.casamento.backend.repository.UsuarioNoivaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AssinaturaService {

    private final SiteRepository siteRepository;
    private final UsuarioNoivaRepository usuarioNoivaRepository;
    private final PasswordEncoder passwordEncoder;
    private final MercadoPagoService mercadoPagoService;
    private final AsaasService asaasService;

    public AssinaturaService(
            SiteRepository siteRepository,
            UsuarioNoivaRepository usuarioNoivaRepository,
            PasswordEncoder passwordEncoder,
            MercadoPagoService mercadoPagoService,
            AsaasService asaasService) {
        this.siteRepository = siteRepository;
        this.usuarioNoivaRepository = usuarioNoivaRepository;
        this.passwordEncoder = passwordEncoder;
        this.mercadoPagoService = mercadoPagoService;
        this.asaasService = asaasService;
    }

    /**
     * Cadastro + checkout de assinatura mensal (Asaas).
     * Se o e-mail já existe com site ainda PENDENTE, reabre o pagamento (não bloqueia).
     */
    @Transactional
    public Map<String, Object> iniciarCheckout(
            String nomeNoiva,
            String nomeNoivo,
            String slug,
            String email,
            String senha,
            String cpf,
            String emailPagador) {

        String slugNorm = normalizarSlug(slug);
        String emailNorm = email.trim().toLowerCase();
        String emailCobranca = normalizarEmailPagador(emailPagador, emailNorm);

        if (slugNorm.isBlank()) {
            throw new IllegalArgumentException("Informe um slug (ex: mariajoao).");
        }
        if (senha == null || senha.length() < 6) {
            throw new IllegalArgumentException("A senha precisa ter pelo menos 6 caracteres.");
        }
        if (emailCobranca.isBlank() || !emailCobranca.contains("@")) {
            throw new IllegalArgumentException("Informe um e-mail válido para a cobrança.");
        }
        if (!asaasService.configurado()) {
            throw new IllegalStateException("Checkout indisponível: Asaas não configurado no servidor.");
        }

        var usuarioExistente = usuarioNoivaRepository.findByEmailIgnoreCase(emailNorm);
        if (usuarioExistente.isPresent()) {
            Site siteExistente = usuarioExistente.get().getSite();
            if (siteJaPagoOuAtivo(siteExistente)) {
                throw new IllegalArgumentException(
                        "Já existe uma conta ativa com este e-mail. Entre no painel para continuar.");
            }
            return retomarCheckoutPendente(
                    usuarioExistente.get(), siteExistente, nomeNoiva, nomeNoivo, slugNorm, senha, cpf, emailCobranca);
        }

        if (siteRepository.findBySlug(slugNorm).isPresent()) {
            throw new IllegalArgumentException("Este link já está em uso. Escolha outro (ex: maria-e-joao).");
        }
        if (slugReservado(slugNorm)) {
            throw new IllegalArgumentException("Este link é reservado. Escolha outro (ex: maria-e-joao).");
        }

        Site site = new Site();
        site.setSlug(slugNorm);
        site.setNomeNoiva(nomeNoiva.trim());
        site.setNomeNoivo(nomeNoivo.trim());
        site.setNomeCurto(nomeNoiva.trim().split("\\s+")[0] + " & " + nomeNoivo.trim().split("\\s+")[0]);
        site.setVersiculo("\"Assim, eles já não são dois, mas sim uma só carne. Portanto, o que Deus uniu, ninguém separe.\" Mateus 19:6");
        site.setFraseBencao("Com a bênção de Deus e nossos pais");
        site.setTituloGaleria("Nossos momentos");
        site.setHistoriaCurta("Um encontro, um sim e o começo da nossa história juntos.");
        site.setAtivo(false);
        site.setAssinaturaStatus("PENDENTE");
        site = siteRepository.save(site);

        UsuarioNoiva usuario = new UsuarioNoiva();
        usuario.setEmail(emailNorm);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setSite(site);
        usuarioNoivaRepository.save(usuario);

        return gerarCheckoutAssinatura(site, cpf, emailCobranca);
    }

    private Map<String, Object> retomarCheckoutPendente(
            UsuarioNoiva usuario,
            Site site,
            String nomeNoiva,
            String nomeNoivo,
            String slugNorm,
            String senha,
            String cpf,
            String emailCobranca) {

        if (!slugNorm.equalsIgnoreCase(site.getSlug())) {
            if (slugReservado(slugNorm)) {
                throw new IllegalArgumentException("Este link é reservado. Escolha outro (ex: maria-e-joao).");
            }
            var outro = siteRepository.findBySlug(slugNorm);
            if (outro.isPresent() && !outro.get().getId().equals(site.getId())) {
                throw new IllegalArgumentException("Este link já está em uso. Escolha outro (ex: maria-e-joao).");
            }
            site.setSlug(slugNorm);
        }

        site.setNomeNoiva(nomeNoiva.trim());
        site.setNomeNoivo(nomeNoivo.trim());
        site.setNomeCurto(nomeNoiva.trim().split("\\s+")[0] + " & " + nomeNoivo.trim().split("\\s+")[0]);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuarioNoivaRepository.save(usuario);
        siteRepository.save(site);

        return gerarCheckoutAssinatura(site, cpf, emailCobranca);
    }

    private static String normalizarEmailPagador(String emailPagador, String emailLogin) {
        if (emailPagador != null && !emailPagador.isBlank()) {
            return emailPagador.trim().toLowerCase();
        }
        return emailLogin == null ? "" : emailLogin.trim().toLowerCase();
    }

    private Map<String, Object> gerarCheckoutAssinatura(Site site, String cpf, String emailCobranca) {
        String nomeCliente = site.getNomeNoiva() + " & " + site.getNomeNoivo();
        String subExistente = site.getMpPreapprovalId();
        if (subExistente != null && !subExistente.startsWith("sub_")) {
            subExistente = null;
        }

        Map<String, String> assinatura = asaasService.criarAssinaturaMensal(
                site.getId(),
                site.getSlug(),
                nomeCliente,
                emailCobranca,
                cpf,
                subExistente);

        String subId = assinatura.get("id");
        String customerId = assinatura.get("customer_id");
        if (customerId != null && !customerId.isBlank()) {
            site.setMpPreferenceId(customerId);
        }
        if (subId != null && !subId.isBlank()) {
            site.setMpPreapprovalId(subId);
        }
        site.setMpPaymentId("email:" + emailCobranca);
        site.setMpAssinaturaInitPoint(assinatura.get("init_point"));
        site.setMpAssinaturaStatus(assinatura.getOrDefault("status", "ACTIVE"));
        site.setAssinaturaStatus("PENDENTE");
        site.setAtivo(false);
        siteRepository.save(site);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("siteId", site.getId());
        out.put("slug", site.getSlug());
        out.put("checkoutUrl", assinatura.get("init_point"));
        out.put("valorMensal", asaasService.getValorMensal());
        out.put("permanenciaMinimaMeses", asaasService.getPermanenciaMinimaMeses());
        out.put("cancelamentoLivre", asaasService.getPermanenciaMinimaMeses() <= 0);
        out.put("gateway", "asaas");
        out.put("mensagem", "Pague a assinatura de R$ "
                + asaasService.getValorMensal()
                + "/mês (PIX, boleto ou cartão). Sem fidelidade — cancele quando quiser.");
        out.put("modoTeste", asaasService.modoSandbox());
        out.put("retomado", true);
        return out;
    }

    private static boolean siteJaPagoOuAtivo(Site site) {
        if (site.isAtivo()) {
            return true;
        }
        String st = site.getAssinaturaStatus();
        if (st != null && "ATIVA".equalsIgnoreCase(st)) {
            return true;
        }
        String mp = site.getMpAssinaturaStatus();
        return mp != null && ("authorized".equalsIgnoreCase(mp) || "ACTIVE".equalsIgnoreCase(mp));
    }

    /** Webhook Asaas — ativa/desativa conforme eventos de cobrança da assinatura. */
    @Transactional
    public void processarWebhookAsaas(String event, JsonNode payload) {
        if (event == null || event.isBlank()) {
            return;
        }
        String ev = event.trim().toUpperCase();

        if (ev.startsWith("PAYMENT_")) {
            JsonNode payment = payload.path("payment");
            if (payment.isMissingNode() || payment.isNull()) {
                return;
            }
            processarPagamentoAsaas(ev, payment);
            return;
        }

        if (ev.startsWith("SUBSCRIPTION_")) {
            JsonNode sub = payload.path("subscription");
            String subId = sub.path("id").asText("");
            if (subId.isBlank()) {
                subId = payload.path("id").asText("");
            }
            if (subId.isBlank()) {
                return;
            }
            Site site = siteRepository.findByMpPreapprovalId(subId).orElse(null);
            if (site == null || slugReservado(site.getSlug())) {
                return;
            }
            if ("SUBSCRIPTION_DELETED".equals(ev)
                    || "SUBSCRIPTION_INACTIVATED".equals(ev)
                    || "SUBSCRIPTION_EXPIRED".equals(ev)) {
                site.setAtivo(false);
                site.setAssinaturaStatus("CANCELADA");
                site.setMpAssinaturaStatus("INACTIVE");
                siteRepository.save(site);
            }
        }
    }

    private void processarPagamentoAsaas(String event, JsonNode payment) {
        String external = payment.path("externalReference").asText("");
        String subscriptionId = payment.path("subscription").asText("");
        String paymentId = payment.path("id").asText("");
        String status = payment.path("status").asText("");

        Site site = resolverSitePorExternal(external);
        if (site == null && !subscriptionId.isBlank()) {
            site = siteRepository.findByMpPreapprovalId(subscriptionId).orElse(null);
        }
        if (site == null || slugReservado(site.getSlug())) {
            return;
        }

        if (!subscriptionId.isBlank()) {
            site.setMpPreapprovalId(subscriptionId);
        }

        boolean pago = "PAYMENT_CONFIRMED".equals(event)
                || "PAYMENT_RECEIVED".equals(event)
                || "RECEIVED".equalsIgnoreCase(status)
                || "CONFIRMED".equalsIgnoreCase(status)
                || "RECEIVED_IN_CASH".equalsIgnoreCase(status);

        boolean falha = "PAYMENT_OVERDUE".equals(event)
                || "PAYMENT_DELETED".equals(event)
                || "PAYMENT_REFUNDED".equals(event)
                || "PAYMENT_CHARGEBACK_REQUESTED".equals(event)
                || "PAYMENT_CHARGEBACK_DISPUTE".equals(event)
                || "PAYMENT_AWAITING_CHARGEBACK_REVERSAL".equals(event)
                || "PAYMENT_DUNNING_REQUESTED".equals(event)
                || "REFUNDED".equalsIgnoreCase(status)
                || "CHARGEBACK".equalsIgnoreCase(status);

        if (pago) {
            ativarSite(site, paymentId);
            site.setMpAssinaturaStatus("ACTIVE");
            siteRepository.save(site);
        } else if (falha && site.isAtivo()) {
            // Atraso/estorno só derruba se já estava ativo (renovação)
            desativarPorFaltaPagamento(site.getId());
        } else if ("PAYMENT_OVERDUE".equals(event) && "PENDENTE".equalsIgnoreCase(site.getAssinaturaStatus())) {
            site.setMpAssinaturaStatus("OVERDUE");
            siteRepository.save(site);
        }
    }

    // --- Mercado Pago (legado / presentes) ---

    @Transactional
    public void processarPagamentoAprovado(String paymentId) {
        JsonNode pagamento = mercadoPagoService.buscarPagamento(paymentId);
        String status = pagamento.path("status").asText("");

        String external = pagamento.path("external_reference").asText("");
        if (external.startsWith("pedido:")) {
            return;
        }
        Site site = resolverSitePorExternal(external);

        if (site == null) {
            String preapprovalId = pagamento.path("metadata").path("preapproval_id").asText("");
            if (!preapprovalId.isBlank()) {
                site = siteRepository.findByMpPreapprovalId(preapprovalId).orElse(null);
            }
        }

        if (site == null || slugReservado(site.getSlug())) {
            return;
        }

        if ("approved".equalsIgnoreCase(status)) {
            ativarSite(site, paymentId);
        } else if (isStatusFalhaPagamento(status)) {
            desativarPorFaltaPagamento(site.getId());
        }
    }

    @Transactional
    public void processarPreapproval(String preapprovalId) {
        JsonNode pre = mercadoPagoService.buscarPreapproval(preapprovalId);
        String status = pre.path("status").asText("");
        String external = pre.path("external_reference").asText("");
        String planId = pre.path("preapproval_plan_id").asText("");
        String payerEmail = pre.path("payer_email").asText("");
        if (payerEmail.isBlank()) {
            payerEmail = pre.path("payer").path("email").asText("");
        }

        Site site = resolverSitePorExternal(external);
        if (site == null) {
            site = siteRepository.findByMpPreapprovalId(preapprovalId).orElse(null);
        }
        if (site == null && !payerEmail.isBlank()) {
            site = siteRepository
                    .findFirstByMpPaymentIdIgnoreCaseAndAssinaturaStatusOrderByIdDesc(
                            "email:" + payerEmail.trim().toLowerCase(), "PENDENTE")
                    .orElse(null);
        }
        if (site == null && !planId.isBlank()) {
            site = siteRepository
                    .findFirstByMpPreferenceIdAndAssinaturaStatusOrderByIdDesc(planId, "PENDENTE")
                    .orElse(null);
        }
        if (site == null && !payerEmail.isBlank()) {
            site = usuarioNoivaRepository.findByEmailIgnoreCase(payerEmail)
                    .map(UsuarioNoiva::getSite)
                    .orElse(null);
        }
        if (site == null || slugReservado(site.getSlug())) {
            return;
        }

        site.setMpPreapprovalId(preapprovalId);
        site.setMpAssinaturaStatus(status);
        if (!planId.isBlank()) {
            site.setMpPreferenceId(planId);
        }
        String init = pre.path("init_point").asText("");
        if (!init.isBlank()) {
            site.setMpAssinaturaInitPoint(init);
        }

        if ("authorized".equalsIgnoreCase(status)) {
            site.setAtivo(true);
            site.setAssinaturaStatus("ATIVA");
            if (site.getMpPaymentId() != null && site.getMpPaymentId().startsWith("email:")) {
                site.setMpPaymentId(null);
            }
            if (site.getAssinaturaInicio() == null) {
                site.setAssinaturaInicio(Instant.now());
            }
        } else if ("paused".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status)) {
            site.setAtivo(false);
            site.setAssinaturaStatus("cancelled".equalsIgnoreCase(status) ? "CANCELADA" : "ATRASADA");
        }

        siteRepository.save(site);
    }

    @Transactional
    public void processarAuthorizedPayment(String authorizedPaymentId) {
        JsonNode ap = mercadoPagoService.buscarAuthorizedPayment(authorizedPaymentId);
        String preapprovalId = ap.path("preapproval_id").asText("");
        String paymentStatus = ap.path("payment").path("status").asText(ap.path("status").asText(""));

        if (preapprovalId.isBlank()) {
            return;
        }

        Site site = siteRepository.findByMpPreapprovalId(preapprovalId).orElse(null);
        if (site == null || slugReservado(site.getSlug())) {
            return;
        }

        if ("approved".equalsIgnoreCase(paymentStatus) || "processed".equalsIgnoreCase(paymentStatus)) {
            site.setAtivo(true);
            site.setAssinaturaStatus("ATIVA");
            if (site.getAssinaturaInicio() == null) {
                site.setAssinaturaInicio(Instant.now());
            }
            siteRepository.save(site);
        } else if (isStatusFalhaPagamento(paymentStatus)) {
            desativarPorFaltaPagamento(site.getId());
        }
    }

    @Transactional
    public void desativarPorFaltaPagamento(Long siteId) {
        siteRepository.findById(siteId).ifPresent(site -> {
            if (slugReservado(site.getSlug())) {
                return;
            }
            site.setAtivo(false);
            site.setAssinaturaStatus("ATRASADA");
            siteRepository.save(site);
        });
    }

    private void ativarSite(Site site, String paymentId) {
        site.setAtivo(true);
        site.setAssinaturaStatus("ATIVA");
        if (paymentId != null && !paymentId.isBlank()) {
            site.setMpPaymentId(paymentId);
        }
        if (site.getAssinaturaInicio() == null) {
            site.setAssinaturaInicio(Instant.now());
        }
        siteRepository.save(site);
    }

    private Site resolverSitePorExternal(String external) {
        if (external == null || !external.startsWith("site:")) {
            return null;
        }
        try {
            Long siteId = Long.parseLong(external.substring("site:".length()).trim());
            return siteRepository.findById(siteId).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isStatusFalhaPagamento(String status) {
        if (status == null || status.isBlank()) return false;
        String s = status.toLowerCase();
        return s.equals("rejected")
                || s.equals("cancelled")
                || s.equals("canceled")
                || s.equals("refunded")
                || s.equals("charged_back");
    }

    private static String normalizarSlug(String slug) {
        if (slug == null) return "";
        return slug.trim().toLowerCase().replaceAll("[^a-z0-9-]", "");
    }

    private static boolean slugReservado(String slug) {
        return "rafaekevin".equals(slug) || "modelo".equals(slug) || "admin".equals(slug);
    }
}
