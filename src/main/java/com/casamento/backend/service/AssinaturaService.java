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
        out.put("mensagem", "Conclua o pagamento de R$ "
                + mercadoPagoService.getValorCriacao()
                + " para liberar seu site (criação + 1º mês).");
        return out;
    }

    @Transactional
    public void processarPagamentoAprovado(String paymentId) {
        JsonNode pagamento = mercadoPagoService.buscarPagamento(paymentId);
        String status = pagamento.path("status").asText("");
        if (!"approved".equalsIgnoreCase(status)) {
            return;
        }

        String external = pagamento.path("external_reference").asText("");
        if (!external.startsWith("site:")) {
            return;
        }
        Long siteId = Long.parseLong(external.substring("site:".length()).trim());
        Site site = siteRepository.findById(siteId).orElse(null);
        if (site == null) {
            return;
        }

        site.setAtivo(true);
        site.setAssinaturaStatus("ATIVA");
        site.setMpPaymentId(paymentId);
        if (site.getAssinaturaInicio() == null) {
            site.setAssinaturaInicio(Instant.now());
        }
        siteRepository.save(site);
    }

    @Transactional
    public void desativarPorFaltaPagamento(Long siteId) {
        siteRepository.findById(siteId).ifPresent(site -> {
            // Não desliga o site modelo
            if ("rafaekevin".equalsIgnoreCase(site.getSlug())) {
                return;
            }
            site.setAtivo(false);
            site.setAssinaturaStatus("ATRASADA");
            siteRepository.save(site);
        });
    }

    private static String normalizarSlug(String slug) {
        if (slug == null) return "";
        return slug.trim().toLowerCase().replaceAll("[^a-z0-9-]", "");
    }
}
