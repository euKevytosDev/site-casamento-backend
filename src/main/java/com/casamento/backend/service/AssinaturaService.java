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
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AssinaturaService {

    private final SiteRepository siteRepository;
    private final UsuarioNoivaRepository usuarioNoivaRepository;
    private final PasswordEncoder passwordEncoder;
    private final MercadoPagoService mercadoPagoService;
    private final AsaasService asaasService;
    private final PresentesPadraoService presentesPadraoService;
    private final String sitePublicBaseUrl;
    private final String adminFrontUrl;

    public AssinaturaService(
            SiteRepository siteRepository,
            UsuarioNoivaRepository usuarioNoivaRepository,
            PasswordEncoder passwordEncoder,
            MercadoPagoService mercadoPagoService,
            AsaasService asaasService,
            PresentesPadraoService presentesPadraoService,
            @org.springframework.beans.factory.annotation.Value("${app.site-public-base-url:https://somosloven.com.br}")
            String sitePublicBaseUrl,
            @org.springframework.beans.factory.annotation.Value("${mercadopago.admin-front-url:https://somosloven.com.br/admin/}")
            String adminFrontUrl) {
        this.siteRepository = siteRepository;
        this.usuarioNoivaRepository = usuarioNoivaRepository;
        this.passwordEncoder = passwordEncoder;
        this.mercadoPagoService = mercadoPagoService;
        this.asaasService = asaasService;
        this.presentesPadraoService = presentesPadraoService;
        this.sitePublicBaseUrl = sitePublicBaseUrl == null ? "https://somosloven.com.br" : sitePublicBaseUrl.trim();
        this.adminFrontUrl = adminFrontUrl == null || adminFrontUrl.isBlank()
                ? "https://somosloven.com.br/admin/"
                : adminFrontUrl.trim();
    }

    /**
     * Cadastro + checkout de assinatura mensal (Asaas).
     * plano: "trial" (14 dias + cartão, cobra no 15º) | "mensal" (cobra na hora).
     */
    @Transactional
    public Map<String, Object> iniciarCheckout(
            String nomeNoiva,
            String nomeNoivo,
            String slug,
            String email,
            String senha,
            String cpf,
            String emailPagador,
            String plano) {

        String slugNorm = normalizarSlug(slug);
        String emailNorm = email.trim().toLowerCase();
        String emailCobranca = normalizarEmailPagador(emailPagador, emailNorm);
        boolean quererTrial = !"mensal".equalsIgnoreCase(plano == null ? "" : plano.trim());

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
                    usuarioExistente.get(), siteExistente, nomeNoiva, nomeNoivo, slugNorm, senha, cpf, emailCobranca, quererTrial);
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
        presentesPadraoService.garantirPresentesPadrao(site);

        UsuarioNoiva usuario = new UsuarioNoiva();
        usuario.setEmail(emailNorm);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setSite(site);
        usuarioNoivaRepository.save(usuario);

        return gerarCheckoutAssinatura(site, cpf, emailCobranca, quererTrial);
    }

    private Map<String, Object> retomarCheckoutPendente(
            UsuarioNoiva usuario,
            Site site,
            String nomeNoiva,
            String nomeNoivo,
            String slugNorm,
            String senha,
            String cpf,
            String emailCobranca,
            boolean quererTrial) {

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

        return gerarCheckoutAssinatura(site, cpf, emailCobranca, quererTrial);
    }

    private static String normalizarEmailPagador(String emailPagador, String emailLogin) {
        if (emailPagador != null && !emailPagador.isBlank()) {
            return emailPagador.trim().toLowerCase();
        }
        return emailLogin == null ? "" : emailLogin.trim().toLowerCase();
    }

    private Map<String, Object> gerarCheckoutAssinatura(Site site, String cpf, String emailCobranca, boolean quererTrial) {
        String nomeCliente = site.getNomeNoiva() + " & " + site.getNomeNoivo();
        String subExistente = site.getMpPreapprovalId();
        if (subExistente != null && !subExistente.startsWith("sub_")) {
            subExistente = null;
        }

        boolean trialJaUsado = "ATRASADA".equalsIgnoreCase(site.getAssinaturaStatus())
                || (site.getTrialAte() != null && site.getTrialAte().isBefore(LocalDate.now()));
        boolean usarTrial = quererTrial && !trialJaUsado && asaasService.getTrialDias() > 0;

        Map<String, String> assinatura = asaasService.criarAssinaturaMensal(
                site.getId(),
                site.getSlug(),
                nomeCliente,
                emailCobranca,
                cpf,
                trialJaUsado ? null : subExistente,
                !usarTrial);

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

        int trialDias = asaasService.getTrialDias();
        if (!"ATIVA".equalsIgnoreCase(site.getAssinaturaStatus())
                && !"TRIAL".equalsIgnoreCase(site.getAssinaturaStatus())) {
            prepararCheckoutPendente(site, usarTrial, trialDias, assinatura.get("first_due_date"));
        }

        siteRepository.save(site);

        String base = sitePublicBaseUrl.replaceAll("/$", "");
        String painel = adminFrontUrl.contains("painel")
                ? adminFrontUrl.replace("painel.html", "index.html")
                : (adminFrontUrl.endsWith("/") ? adminFrontUrl : adminFrontUrl + "/");
        if (!painel.contains("/admin")) {
            painel = base + "/admin/";
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("siteId", site.getId());
        out.put("slug", site.getSlug());
        out.put("checkoutUrl", assinatura.get("init_point"));
        out.put("painelUrl", painel);
        out.put("siteUrl", base + "/" + site.getSlug());
        out.put("valorMensal", asaasService.getValorMensal());
        out.put("permanenciaMinimaMeses", asaasService.getPermanenciaMinimaMeses());
        out.put("cancelamentoLivre", asaasService.getPermanenciaMinimaMeses() <= 0);
        out.put("trialDias", usarTrial ? trialDias : 0);
        out.put("trialAte", site.getTrialAte() != null ? site.getTrialAte().toString() : null);
        out.put("emTrial", false); // só libera depois de cadastrar o cartão no Asaas
        out.put("exigeCartao", true);
        out.put("plano", usarTrial ? "trial" : "mensal");
        out.put("gateway", "asaas");
        if (usarTrial) {
            out.put("mensagem", "Cadastre o cartão para liberar "
                    + trialDias + " dias grátis. A 1ª cobrança de R$ "
                    + asaasService.getValorMensal() + " será no 15º dia.");
        } else {
            out.put("mensagem", "Assine com cartão — R$ "
                    + asaasService.getValorMensal()
                    + "/mês. Sem fidelidade — cancele quando quiser.");
        }
        out.put("modoTeste", asaasService.modoSandbox());
        out.put("retomado", true);
        return out;
    }

    /**
     * Marca trialAte, mas só libera o site depois do cartão (webhook PENDING/CONFIRMED).
     */
    private void prepararCheckoutPendente(Site site, boolean usarTrial, int trialDias, String firstDueDateIso) {
        site.setAssinaturaStatus("PENDENTE");
        site.setAtivo(false);
        if (!usarTrial || trialDias <= 0) {
            return;
        }
        if (site.getAssinaturaInicio() == null) {
            site.setAssinaturaInicio(Instant.now());
        }
        if (site.getTrialAte() == null) {
            LocalDate ate = null;
            if (firstDueDateIso != null && !firstDueDateIso.isBlank()) {
                try {
                    ate = LocalDate.parse(firstDueDateIso.trim());
                } catch (Exception ignored) {
                    ate = null;
                }
            }
            if (ate == null) {
                ate = LocalDate.now().plusDays(trialDias);
            }
            site.setTrialAte(ate);
        }
    }

    private static boolean siteJaPagoOuAtivo(Site site) {
        if (site == null) {
            return false;
        }
        String st = site.getAssinaturaStatus();
        if (st != null) {
            if ("ATIVA".equalsIgnoreCase(st) || "TRIAL".equalsIgnoreCase(st)) {
                return true;
            }
            if ("PENDENTE".equalsIgnoreCase(st)
                    || "ATRASADA".equalsIgnoreCase(st)
                    || "CANCELADA".equalsIgnoreCase(st)) {
                return false;
            }
        }
        return site.isAtivo();
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

        boolean cartaoCadastradoAguardandoCobranca = "PAYMENT_CREATED".equals(event)
                || "PAYMENT_UPDATED".equals(event)
                || "PENDING".equalsIgnoreCase(status)
                || "AWAITING_PAYMENT".equalsIgnoreCase(status)
                || "AWAITING_RISK_ANALYSIS".equalsIgnoreCase(status);

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
        } else if (cartaoCadastradoAguardandoCobranca
                && site.getTrialAte() != null
                && !site.getTrialAte().isBefore(LocalDate.now())
                && !"ATIVA".equalsIgnoreCase(site.getAssinaturaStatus())) {
            // Cartão cadastrado no trial: libera site até a 1ª cobrança (dia 15)
            site.setAtivo(true);
            site.setAssinaturaStatus("TRIAL");
            site.setMpAssinaturaStatus("PENDING");
            if (site.getAssinaturaInicio() == null) {
                site.setAssinaturaInicio(Instant.now());
            }
            if (paymentId != null && !paymentId.isBlank()) {
                site.setMpPaymentId(paymentId);
            }
            siteRepository.save(site);
            presentesPadraoService.garantirPresentesPadrao(site);
        } else if (falha && site.isAtivo()) {
            desativarPorFaltaPagamento(site.getId());
        } else if ("PAYMENT_OVERDUE".equals(event)
                && ("PENDENTE".equalsIgnoreCase(site.getAssinaturaStatus())
                || "TRIAL".equalsIgnoreCase(site.getAssinaturaStatus()))) {
            site.setMpAssinaturaStatus("OVERDUE");
            site.setAtivo(false);
            site.setAssinaturaStatus("ATRASADA");
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
        if (site.isAtivo()) {
            presentesPadraoService.garantirPresentesPadrao(site);
        }
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
            presentesPadraoService.garantirPresentesPadrao(site);
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

    /** Encerra trials vencidos que ainda não pagaram (rede de segurança além do webhook Asaas). */
    @Transactional
    public int encerrarTrialsVencidos() {
        var vencidos = siteRepository.findByAssinaturaStatusAndTrialAteBefore("TRIAL", LocalDate.now());
        int n = 0;
        for (Site site : vencidos) {
            if (slugReservado(site.getSlug())) {
                continue;
            }
            site.setAtivo(false);
            site.setAssinaturaStatus("ATRASADA");
            siteRepository.save(site);
            n++;
        }
        return n;
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
        // Sites criados antes do seed: preenche só se ainda estiver vazio
        presentesPadraoService.garantirPresentesPadrao(site);
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
