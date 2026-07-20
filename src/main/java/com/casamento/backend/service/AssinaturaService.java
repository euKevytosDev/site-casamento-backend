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

    /**
     * Cadastro + checkout de assinatura mensal.
     * Se o e-mail já existe com site ainda PENDENTE, reabre o pagamento (não bloqueia).
     */
    @Transactional
    public Map<String, Object> iniciarCheckout(
            String nomeNoiva,
            String nomeNoivo,
            String slug,
            String email,
            String senha,
            String emailPagador) {

        String slugNorm = normalizarSlug(slug);
        String emailNorm = email.trim().toLowerCase();
        String emailMp = normalizarEmailPagador(emailPagador, emailNorm);

        if (slugNorm.isBlank()) {
            throw new IllegalArgumentException("Informe um slug (ex: mariajoao).");
        }
        if (senha == null || senha.length() < 6) {
            throw new IllegalArgumentException("A senha precisa ter pelo menos 6 caracteres.");
        }
        if (emailMp.isBlank() || !emailMp.contains("@")) {
            throw new IllegalArgumentException("Informe um e-mail válido para o pagamento no Mercado Pago.");
        }

        var usuarioExistente = usuarioNoivaRepository.findByEmailIgnoreCase(emailNorm);
        if (usuarioExistente.isPresent()) {
            Site siteExistente = usuarioExistente.get().getSite();
            if (siteJaPagoOuAtivo(siteExistente)) {
                throw new IllegalArgumentException(
                        "Já existe uma conta ativa com este e-mail. Entre no painel para continuar.");
            }
            return retomarCheckoutPendente(
                    usuarioExistente.get(), siteExistente, nomeNoiva, nomeNoivo, slugNorm, senha, emailMp);
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

        return gerarCheckoutAssinatura(site, emailMp);
    }

    private Map<String, Object> retomarCheckoutPendente(
            UsuarioNoiva usuario,
            Site site,
            String nomeNoiva,
            String nomeNoivo,
            String slugNorm,
            String senha,
            String emailMp) {

        // Permite trocar o slug só se estiver livre (ou for o mesmo do site)
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

        return gerarCheckoutAssinatura(site, emailMp);
    }

    /** E-mail do pagador no MP (pode ser de quem paga com cartão); senão usa o login do painel. */
    private static String normalizarEmailPagador(String emailPagador, String emailLogin) {
        if (emailPagador != null && !emailPagador.isBlank()) {
            return emailPagador.trim().toLowerCase();
        }
        return emailLogin == null ? "" : emailLogin.trim().toLowerCase();
    }

    private Map<String, Object> gerarCheckoutAssinatura(Site site, String emailMp) {
        Map<String, String> assinatura = mercadoPagoService.criarAssinaturaMensal(
                site.getId(), site.getSlug(), emailMp);

        String id = assinatura.get("id");
        String planId = assinatura.get("plan_id");
        if (planId != null && !planId.isBlank()) {
            site.setMpPreferenceId(planId);
        }
        // id "plan:xxx" = ainda não existe preapproval; webhook vai preencher
        if (id != null && !id.startsWith("plan:")) {
            site.setMpPreapprovalId(id);
        }
        // Reusa mp_payment_id só enquanto PENDENTE, sem criar coluna nova no banco
        site.setMpPaymentId("email:" + emailMp);
        site.setMpAssinaturaInitPoint(assinatura.get("init_point"));
        site.setMpAssinaturaStatus(assinatura.getOrDefault("status", "pending"));
        site.setAssinaturaStatus("PENDENTE");
        site.setAtivo(false);
        siteRepository.save(site);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("siteId", site.getId());
        out.put("slug", site.getSlug());
        out.put("checkoutUrl", assinatura.get("init_point"));
        out.put("valorMensal", mercadoPagoService.getValorMensal());
        out.put("permanenciaMinimaMeses", mercadoPagoService.getPermanenciaMinimaMeses());
        out.put("cancelamentoLivre", mercadoPagoService.getPermanenciaMinimaMeses() <= 0);
        out.put("mensagem", "Autorize a assinatura de R$ "
                + mercadoPagoService.getValorMensal()
                + "/mês no Mercado Pago. Sem fidelidade — cancele quando quiser.");
        out.put("modoTeste", mercadoPagoService.modoTeste());
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
        return mp != null && "authorized".equalsIgnoreCase(mp);
    }

    @Transactional
    public void processarPagamentoAprovado(String paymentId) {
        JsonNode pagamento = mercadoPagoService.buscarPagamento(paymentId);
        String status = pagamento.path("status").asText("");

        String external = pagamento.path("external_reference").asText("");
        if (external.startsWith("pedido:")) {
            return; // presente do casal — tratado em PresenteService
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

    /** Slugs do produto (vitrine + template) — não podem ser usados por cliente. */
    private static boolean slugReservado(String slug) {
        return "rafaekevin".equals(slug) || "modelo".equals(slug) || "admin".equals(slug);
    }
}
