package com.casamento.backend.controller;

import com.casamento.backend.config.SiteContext;
import com.casamento.backend.model.Site;
import com.casamento.backend.repository.SiteRepository;
import com.casamento.backend.service.MercadoPagoService;
import com.casamento.backend.service.PresenteService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Conexão da conta Mercado Pago da noiva (OAuth) + status no painel.
 */
@RestController
@RequestMapping("/api/admin/mercadopago")
@CrossOrigin(origins = "*")
public class AdminMercadoPagoController {

    private final MercadoPagoService mercadoPagoService;
    private final PresenteService presenteService;
    private final SiteRepository siteRepository;
    private final String stateSecret;

    public AdminMercadoPagoController(
            MercadoPagoService mercadoPagoService,
            PresenteService presenteService,
            SiteRepository siteRepository,
            @Value("${jwt.secret:casamento-dev-secret-change-me}") String stateSecret) {
        this.mercadoPagoService = mercadoPagoService;
        this.presenteService = presenteService;
        this.siteRepository = siteRepository;
        this.stateSecret = stateSecret;
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        Site site = SiteContext.get();
        if (site == null) {
            return ResponseEntity.badRequest().body("Informe o header X-Site-Id.");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("conectado", site.isMpSellerConectado());
        out.put("oauthDisponivel", mercadoPagoService.oauthConfigurado());
        out.put("userId", site.getMpSellerUserId());
        out.put("conectadoEm", site.getMpSellerConectadoEm() != null ? site.getMpSellerConectadoEm().toString() : null);
        out.put("permiteTokenManual", true);
        return ResponseEntity.ok(out);
    }

    @GetMapping("/oauth/url")
    public ResponseEntity<?> oauthUrl() {
        Site site = SiteContext.get();
        if (site == null) {
            return ResponseEntity.badRequest().body("Informe o header X-Site-Id.");
        }
        try {
            String state = assinarState(site.getSlug());
            String url = mercadoPagoService.montarUrlAutorizacaoOAuth(state);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Callback do browser após a noiva autorizar no MP (sem JWT). */
    @GetMapping("/oauth/callback")
    public RedirectView oauthCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {

        String front = mercadoPagoService.getAdminFrontUrl();
        if (error != null && !error.isBlank()) {
            return new RedirectView(front + "?mp=erro");
        }
        if (code == null || code.isBlank() || state == null || state.isBlank()) {
            return new RedirectView(front + "?mp=erro");
        }

        try {
            String slug = validarState(state);
            Site site = siteRepository.findBySlug(slug)
                    .orElseThrow(() -> new IllegalArgumentException("Site não encontrado."));
            Map<String, String> tokens = mercadoPagoService.trocarCodePorTokens(code);
            presenteService.conectarSeller(
                    site,
                    tokens.get("access_token"),
                    tokens.get("refresh_token"),
                    tokens.get("user_id")
            );
            return new RedirectView(front + "?mp=ok");
        } catch (Exception e) {
            return new RedirectView(front + "?mp=erro");
        }
    }

    /** Fallback: colar Access Token da conta da noiva (Teste/Produção). */
    @PostMapping("/token-manual")
    public ResponseEntity<?> salvarTokenManual(@RequestBody Map<String, String> body) {
        Site site = SiteContext.get();
        if (site == null) {
            return ResponseEntity.badRequest().body("Informe o header X-Site-Id.");
        }
        String token = body.get("accessToken");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body("Informe o accessToken da conta Mercado Pago.");
        }
        String userId = body.getOrDefault("userId", "");
        presenteService.conectarSeller(site, token.trim(), null, userId);
        return ResponseEntity.ok(Map.of(
                "conectado", true,
                "mensagem", "Mercado Pago conectado. Convidados poderão pagar presentes no cartão."
        ));
    }

    @DeleteMapping("/conexao")
    public ResponseEntity<?> desconectar() {
        Site site = SiteContext.get();
        if (site == null) {
            return ResponseEntity.badRequest().body("Informe o header X-Site-Id.");
        }
        presenteService.desconectarSeller(site);
        return ResponseEntity.ok(Map.of("conectado", false, "mensagem", "Mercado Pago desconectado."));
    }

    private String assinarState(String slug) {
        long exp = Instant.now().getEpochSecond() + 600;
        String payload = slug + "." + exp;
        return payload + "." + hmac(payload);
    }

    private String validarState(String state) {
        String[] parts = state.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("state inválido");
        }
        String slug = parts[0];
        long exp = Long.parseLong(parts[1]);
        String sig = parts[2];
        if (Instant.now().getEpochSecond() > exp) {
            throw new IllegalArgumentException("state expirado");
        }
        String expected = hmac(slug + "." + exp);
        if (!expected.equalsIgnoreCase(sig)) {
            throw new IllegalArgumentException("state inválido");
        }
        return slug.toLowerCase();
    }

    private String hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(stateSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao assinar state OAuth", e);
        }
    }
}
