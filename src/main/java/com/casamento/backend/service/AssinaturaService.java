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
            String senha) {

        String slugNorm = normalizarSlug(slug);
        String emailNorm = email.trim().toLowerCase();

        if (slugNorm.isBlank()) {
            throw new IllegalArgumentException("Informe um slug (ex: mariajoao).");
        }
        if (senha == null || senha.length() < 6) {
            throw new IllegalArgumentException("A senha precisa ter pelo menos 6 caracteres.");
        }

        var usuarioExistente = usuarioNoivaRepository.findByEmailIgnoreCase(emailNorm);
        if (usuarioExistente.isPresent()) {
            Site siteExistente = usuarioExistente.get().getSite();
            if (siteJaPagoOuAtivo(siteExistente)) {
                throw new IllegalArgumentException(
                        "Já existe uma conta ativa com este e-mail. Entre no painel para continuar.");
            }
            return retomarCheckoutPendente(
                    usuarioExistente.get(), siteExistente, nomeNoiva, nomeNoivo, slugNorm, senha);
        }

        if (siteRepository.findBySlug(slugNorm).isPresent()) {
            throw new IllegalArgumentException("Este link já está em uso. Escolha outro (ex: maria-e-joao).");
        }

        Site site = new Site();
        site.setSlug(slugNorm);
        site.setNomeNoiva(nomeNoiva.trim());
        site.setNomeNoivo(nomeNoivo.trim());
        site.setNomeCurto(nomeNoiva.trim().split("\\s+")[0] + " & " + nomeNoivo.trim().split("\\s+")[0]);
        site.setVersiculo("\"Assim, eles já não são dois, mas sim uma só carne. Portanto, o que Deus uniu, ninguém separe.\" Mateus 19:6");
        site.setFraseBencao("Com a bênção de Deus e nossos pais");
        site.setAtivo(false);
        site.setAssinaturaStatus("PENDENTE");
        site = siteRepository.save(site);

        UsuarioNoiva usuario = new UsuarioNoiva();
        usuario.setEmail(emailNorm);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setSite(site);
        usuarioNoivaRepository.save(usuario);

        return gerarCheckoutAssinatura(site, emailNorm);
    }

    private Map<String, Object> retomarCheckoutPendente(
            UsuarioNoiva usuario,
            Site site,
            String nomeNoiva,
            String nomeNoivo,
            String slugNorm,
            String senha) {

        // Permite trocar o slug só se estiver livre (ou for o mesmo do site)
        if (!slugNorm.equalsIgnoreCase(site.getSlug())) {
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

        return gerarCheckoutAssinatura(site, usuario.getEmail());
    }

    private Map<String, Object> gerarCheckoutAssinatura(Site site, String emailNorm) {
        Map<String, String> assinatura = mercadoPagoService.criarAssinaturaMensal(
                site.getId(), site.getSlug(), emailNorm);

        site.setMpPreapprovalId(assinatura.get("id"));
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
        out.put("mensagem", "Autorize a assinatura de R$ "
                + mercadoPagoService.getValorMensal()
                + "/mês no Mercado Pago para liberar seu site.");
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
        Site site = resolverSitePorExternal(external);

        if (site == null) {
            String preapprovalId = pagamento.path("metadata").path("preapproval_id").asText("");
            if (!preapprovalId.isBlank()) {
                site = siteRepository.findByMpPreapprovalId(preapprovalId).orElse(null);
            }
        }

        if (site == null || "rafaekevin".equalsIgnoreCase(site.getSlug())) {
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
        if (site == null || "rafaekevin".equalsIgnoreCase(site.getSlug())) {
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
            if ("rafaekevin".equalsIgnoreCase(site.getSlug())) {
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
}
