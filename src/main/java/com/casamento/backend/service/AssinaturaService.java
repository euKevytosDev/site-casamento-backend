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

    public AssinaturaService(
            SiteRepository siteRepository,
            UsuarioNoivaRepository usuarioNoivaRepository,
            PasswordEncoder passwordEncoder,
            MercadoPagoService mercadoPagoService) {
        this.siteRepository = siteRepository;
        this.usuarioNoivaRepository = usuarioNoivaRepository;
        this.passwordEncoder = passwordEncoder;
        this.mercadoPagoService = mercadoPagoService;
    }

    @Transactional
    public Map<String, Object> iniciarCheckout(
            String nomeNoiva,
            String nomeNoivo,
            String slug,
            String email,
            String senha) {

        String slugNorm = normalizarSlug(slug);
        String emailNorm = email.trim().toLowerCase();

        if (slugNorm.isBlank()) {
            throw new IllegalArgumentException("Informe um slug (ex: mariajoao).");
        }
        if (siteRepository.findBySlug(slugNorm).isPresent()) {
            throw new IllegalArgumentException("Este link já está em uso. Escolha outro (ex: maria-e-joao).");
        }
        if (usuarioNoivaRepository.existsByEmailIgnoreCase(emailNorm)) {
            throw new IllegalArgumentException("Já existe uma conta com este e-mail.");
        }
        if (senha == null || senha.length() < 6) {
            throw new IllegalArgumentException("A senha precisa ter pelo menos 6 caracteres.");
        }

        Site site = new Site();
        site.setSlug(slugNorm);
        site.setNomeNoiva(nomeNoiva.trim());
        site.setNomeNoivo(nomeNoivo.trim());
        site.setNomeCurto(nomeNoiva.trim().split("\\s+")[0] + " & " + nomeNoivo.trim().split("\\s+")[0]);
        site.setAtivo(false);
        site.setAssinaturaStatus("PENDENTE");
        site = siteRepository.save(site);

        UsuarioNoiva usuario = new UsuarioNoiva();
        usuario.setEmail(emailNorm);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setSite(site);
        usuarioNoivaRepository.save(usuario);

        Map<String, String> pref = mercadoPagoService.criarPreferenciaCriacao(
                site.getId(), slugNorm, emailNorm, site.getNomeNoiva(), site.getNomeNoivo());

        site.setMpPreferenceId(pref.get("id"));
        siteRepository.save(site);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("siteId", site.getId());
        out.put("slug", site.getSlug());
        out.put("checkoutUrl", pref.get("init_point"));
        out.put("valorCriacao", mercadoPagoService.getValorCriacao());
        out.put("valorMensal", mercadoPagoService.getValorMensal());
        out.put("mensagem", "Conclua o pagamento de R$ "
                + mercadoPagoService.getValorCriacao()
                + " para liberar seu site. Em seguida autorize a mensalidade de R$ "
                + mercadoPagoService.getValorMensal()
                + " (cobrança a partir do 2º mês).");
        return out;
    }

    /**
     * Após o R$ 99: ativa o site e devolve o link para autorizar a mensalidade.
     */
    @Transactional
    public Map<String, Object> iniciarMensalidadeAposCriacao(String paymentId, String externalReference) {
        Site site = null;

        if (paymentId != null && !paymentId.isBlank()) {
            processarPagamentoAprovado(paymentId);
            JsonNode pagamento = mercadoPagoService.buscarPagamento(paymentId);
            externalReference = pagamento.path("external_reference").asText(externalReference);
        }

        if (externalReference != null && externalReference.startsWith("site:")) {
            Long siteId = Long.parseLong(externalReference.substring("site:".length()).trim());
            site = siteRepository.findById(siteId).orElse(null);
        }

        if (site == null) {
            throw new IllegalArgumentException("Não encontramos seu site. Confirme o pagamento e tente de novo.");
        }

        if (!site.isAtivo()) {
            site.setAtivo(true);
            site.setAssinaturaStatus("ATIVA");
            if (site.getAssinaturaInicio() == null) {
                site.setAssinaturaInicio(Instant.now());
            }
            siteRepository.save(site);
        }

        Map<String, String> pre = garantirPreapprovalMensal(site);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("siteId", site.getId());
        out.put("slug", site.getSlug());
        out.put("ativo", site.isAtivo());
        out.put("assinaturaStatus", site.getAssinaturaStatus());
        out.put("mensalidadeStatus", site.getMpAssinaturaStatus());
        out.put("mensalidadeCheckoutUrl", pre.get("init_point"));
        out.put("valorMensal", mercadoPagoService.getValorMensal());
        out.put("mensagem", "Site liberado. Autorize a mensalidade de R$ "
                + mercadoPagoService.getValorMensal()
                + " — a primeira cobrança é só no 2º mês.");
        return out;
    }

    @Transactional
    public Map<String, String> garantirPreapprovalMensal(Site site) {
        if (site.getMpAssinaturaInitPoint() != null && !site.getMpAssinaturaInitPoint().isBlank()
                && site.getMpPreapprovalId() != null && !site.getMpPreapprovalId().isBlank()) {
            String st = site.getMpAssinaturaStatus();
            if (st == null || "pending".equalsIgnoreCase(st)) {
                return Map.of(
                        "id", site.getMpPreapprovalId(),
                        "init_point", site.getMpAssinaturaInitPoint(),
                        "status", st != null ? st : "pending"
                );
            }
            if ("authorized".equalsIgnoreCase(st)) {
                return Map.of(
                        "id", site.getMpPreapprovalId(),
                        "init_point", site.getMpAssinaturaInitPoint() != null ? site.getMpAssinaturaInitPoint() : "",
                        "status", "authorized"
                );
            }
        }

        String email = usuarioNoivaRepository.findBySiteId(site.getId())
                .map(UsuarioNoiva::getEmail)
                .orElseThrow(() -> new IllegalStateException("Conta da noiva não encontrada para o site."));

        Map<String, String> pre = mercadoPagoService.criarPreapprovalMensal(
                site.getId(),
                site.getSlug(),
                email,
                site.getNomeNoiva(),
                site.getNomeNoivo(),
                site.getAssinaturaInicio() != null ? site.getAssinaturaInicio() : Instant.now()
        );

        site.setMpPreapprovalId(pre.get("id"));
        site.setMpAssinaturaInitPoint(pre.get("init_point"));
        site.setMpAssinaturaStatus(pre.getOrDefault("status", "pending"));
        siteRepository.save(site);
        return pre;
    }

    @Transactional
    public void processarPagamentoAprovado(String paymentId) {
        JsonNode pagamento = mercadoPagoService.buscarPagamento(paymentId);
        String status = pagamento.path("status").asText("");

        String external = pagamento.path("external_reference").asText("");
        Site site = resolverSitePorExternal(external);

        // Pagamento de fatura da assinatura pode vir com preapproval_id
        if (site == null) {
            String preapprovalId = pagamento.path("metadata").path("preapproval_id").asText("");
            if (preapprovalId.isBlank()) {
                preapprovalId = firstNonBlank(
                        pagamento.path("point_of_interaction").path("transaction_data").path("subscription_id").asText(""),
                        ""
                );
            }
            if (!preapprovalId.isBlank()) {
                site = siteRepository.findByMpPreapprovalId(preapprovalId).orElse(null);
            }
        }

        if (site == null) {
            return;
        }
        if ("rafaekevin".equalsIgnoreCase(site.getSlug())) {
            return;
        }

        if ("approved".equalsIgnoreCase(status)) {
            site.setAtivo(true);
            site.setAssinaturaStatus("ATIVA");
            if (site.getMpPaymentId() == null || site.getMpPaymentId().isBlank()) {
                site.setMpPaymentId(paymentId);
            }
            if (site.getAssinaturaInicio() == null) {
                site.setAssinaturaInicio(Instant.now());
            }
            siteRepository.save(site);
            try {
                garantirPreapprovalMensal(site);
            } catch (Exception ignored) {
                // Checkout da mensalidade pode ser gerado depois na página de sucesso
            }
        } else if (isStatusFalhaPagamento(status)) {
            desativarPorFaltaPagamento(site.getId());
        }
    }

    @Transactional
    public void processarPreapproval(String preapprovalId) {
        JsonNode pre = mercadoPagoService.buscarPreapproval(preapprovalId);
        String status = pre.path("status").asText("");
        String external = pre.path("external_reference").asText("");

        Site site = resolverSitePorExternal(external);
        if (site == null) {
            site = siteRepository.findByMpPreapprovalId(preapprovalId).orElse(null);
        }
        if (site == null || "rafaekevin".equalsIgnoreCase(site.getSlug())) {
            return;
        }

        site.setMpPreapprovalId(preapprovalId);
        site.setMpAssinaturaStatus(status);
        String init = pre.path("init_point").asText("");
        if (!init.isBlank()) {
            site.setMpAssinaturaInitPoint(init);
        }

        if ("authorized".equalsIgnoreCase(status)) {
            site.setAtivo(true);
            site.setAssinaturaStatus("ATIVA");
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
        if (site == null || "rafaekevin".equalsIgnoreCase(site.getSlug())) {
            return;
        }

        if ("approved".equalsIgnoreCase(paymentStatus) || "processed".equalsIgnoreCase(paymentStatus)) {
            site.setAtivo(true);
            site.setAssinaturaStatus("ATIVA");
            siteRepository.save(site);
        } else if (isStatusFalhaPagamento(paymentStatus)) {
            desativarPorFaltaPagamento(site.getId());
        }
    }

    @Transactional
    public void desativarPorFaltaPagamento(Long siteId) {
        siteRepository.findById(siteId).ifPresent(site -> {
            if ("rafaekevin".equalsIgnoreCase(site.getSlug())) {
                return;
            }
            site.setAtivo(false);
            site.setAssinaturaStatus("ATRASADA");
            siteRepository.save(site);
        });
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

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    private static String normalizarSlug(String slug) {
        if (slug == null) return "";
        return slug.trim().toLowerCase().replaceAll("[^a-z0-9-]", "");
    }
}
