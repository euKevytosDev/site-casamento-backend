package com.casamento.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Integração Mercado Pago:
 * - Assinatura SaaS (token da plataforma)
 * - Presentes no cartão (token OAuth da noiva)
 */
@Service
public class MercadoPagoService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String accessToken;
    private final String notificationUrl;
    private final String backUrlSuccess;
    private final String backUrlFailure;
    private final BigDecimal valorMensal;
    private final int permanenciaMinimaMeses;
    private final String preapprovalPlanIdConfigurado;
    private final String oauthClientId;
    private final String oauthClientSecret;
    private final String oauthRedirectUri;
    private final String adminFrontUrl;
    private volatile String preapprovalPlanIdCache;

    public MercadoPagoService(
            ObjectMapper objectMapper,
            @Value("${mercadopago.access-token:}") String accessToken,
            @Value("${mercadopago.notification-url:}") String notificationUrl,
            @Value("${mercadopago.back-url-success:}") String backUrlSuccess,
            @Value("${mercadopago.back-url-failure:}") String backUrlFailure,
            @Value("${mercadopago.valor-mensal:59.90}") BigDecimal valorMensal,
            @Value("${mercadopago.permanencia-minima-meses:0}") int permanenciaMinimaMeses,
            @Value("${mercadopago.preapproval-plan-id:}") String preapprovalPlanId,
            @Value("${mercadopago.oauth.client-id:}") String oauthClientId,
            @Value("${mercadopago.oauth.client-secret:}") String oauthClientSecret,
            @Value("${mercadopago.oauth.redirect-uri:}") String oauthRedirectUri,
            @Value("${mercadopago.admin-front-url:https://app.somosloven.com/admin/painel.html}") String adminFrontUrl) {
        this.objectMapper = objectMapper;
        this.accessToken = accessToken == null ? "" : accessToken.trim();
        this.notificationUrl = notificationUrl == null ? "" : notificationUrl.trim();
        this.backUrlSuccess = backUrlSuccess == null ? "" : backUrlSuccess.trim();
        this.backUrlFailure = backUrlFailure == null ? "" : backUrlFailure.trim();
        this.valorMensal = valorMensal;
        this.permanenciaMinimaMeses = permanenciaMinimaMeses;
        this.preapprovalPlanIdConfigurado = preapprovalPlanId == null ? "" : preapprovalPlanId.trim();
        this.oauthClientId = oauthClientId == null ? "" : oauthClientId.trim();
        this.oauthClientSecret = oauthClientSecret == null ? "" : oauthClientSecret.trim();
        this.oauthRedirectUri = oauthRedirectUri == null ? "" : oauthRedirectUri.trim();
        this.adminFrontUrl = adminFrontUrl == null || adminFrontUrl.isBlank()
                ? "https://app.somosloven.com/admin/painel.html"
                : adminFrontUrl.trim();
        this.restClient = RestClient.builder()
                .baseUrl("https://api.mercadopago.com")
                .build();
    }

    public boolean configurado() {
        return !accessToken.isBlank();
    }

    public boolean oauthConfigurado() {
        return !oauthClientId.isBlank() && !oauthClientSecret.isBlank() && !oauthRedirectUri.isBlank();
    }

    public String getAdminFrontUrl() {
        return adminFrontUrl;
    }

    public boolean modoTeste() {
        return accessToken.startsWith("TEST-");
    }

    public BigDecimal getValorMensal() {
        return valorMensal;
    }

    public int getPermanenciaMinimaMeses() {
        return permanenciaMinimaMeses;
    }

    public String getBackUrlSuccess() {
        return backUrlSuccess;
    }

    public String montarUrlAutorizacaoOAuth(String state) {
        if (!oauthConfigurado()) {
            throw new IllegalStateException(
                    "OAuth do Mercado Pago não configurado. Defina MERCADOPAGO_OAUTH_CLIENT_ID, SECRET e REDIRECT_URI.");
        }
        return UriComponentsBuilder
                .fromHttpUrl("https://auth.mercadopago.com.br/authorization")
                .queryParam("client_id", oauthClientId)
                .queryParam("response_type", "code")
                .queryParam("platform_id", "mp")
                .queryParam("state", state)
                .queryParam("redirect_uri", oauthRedirectUri)
                .build(true)
                .toUriString();
    }

    public Map<String, String> trocarCodePorTokens(String code) {
        if (!oauthConfigurado()) {
            throw new IllegalStateException("OAuth do Mercado Pago não configurado.");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", oauthClientId);
        form.add("client_secret", oauthClientSecret);
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", oauthRedirectUri);

        try {
            String resposta = restClient.post()
                    .uri("/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            JsonNode json = objectMapper.readTree(resposta);
            Map<String, String> out = new HashMap<>();
            out.put("access_token", json.path("access_token").asText(""));
            out.put("refresh_token", json.path("refresh_token").asText(""));
            String userId = json.path("user_id").asText("");
            if (userId.isBlank()) {
                userId = String.valueOf(json.path("user_id").asLong(0));
                if ("0".equals(userId)) userId = "";
            }
            out.put("user_id", userId);
            if (out.get("access_token").isBlank()) {
                throw new IllegalStateException("Resposta OAuth sem access_token: " + resposta);
            }
            return out;
        } catch (IllegalStateException e) {
            throw e;
        } catch (org.springframework.web.client.RestClientResponseException e) {
            throw new IllegalStateException("Falha OAuth MP: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível concluir o OAuth do Mercado Pago.", e);
        }
    }

    /** Checkout Pro na conta da noiva (Access Token dela). */
    public Map<String, String> criarPreferenciaPresente(
            String sellerAccessToken,
            Long pedidoId,
            String titulo,
            BigDecimal total,
            String backUrlSuccessPresente,
            String backUrlPendingOrFailure) {

        if (sellerAccessToken == null || sellerAccessToken.isBlank()) {
            throw new IllegalStateException("Conta Mercado Pago da noiva não conectada.");
        }

        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("title", titulo == null || titulo.isBlank() ? "Presente de casamento" : titulo);
        item.put("quantity", 1);
        item.put("currency_id", "BRL");
        item.put("unit_price", total.doubleValue());
        items.add(item);

        Map<String, Object> backUrls = new HashMap<>();
        backUrls.put("success", backUrlSuccessPresente);
        backUrls.put("pending", backUrlPendingOrFailure);
        backUrls.put("failure", backUrlPendingOrFailure);

        Map<String, Object> body = new HashMap<>();
        body.put("items", items);
        body.put("external_reference", "pedido:" + pedidoId);
        body.put("back_urls", backUrls);
        body.put("auto_return", "approved");
        if (notificationUrl != null && !notificationUrl.isBlank()) {
            body.put("notification_url", notificationUrl);
        }
        body.put("statement_descriptor", "PRESENTE");

        String resposta = postMpComToken("/checkout/preferences", body, sellerAccessToken);
        try {
            JsonNode json = objectMapper.readTree(resposta);
            Map<String, String> out = new HashMap<>();
            out.put("id", json.path("id").asText());
            out.put("init_point", escolherInitPoint(json, sellerAccessToken));
            if (out.get("id").isBlank() || out.get("init_point").isBlank()) {
                throw new IllegalStateException("Preferência inválida: " + resposta);
            }
            return out;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível criar o checkout de presente.", e);
        }
    }

    public JsonNode buscarPagamentoComToken(String paymentId, String token) {
        return getJsonComToken("/v1/payments/{id}", paymentId, token);
    }

    public Map<String, String> criarAssinaturaMensal(Long siteId, String slug, String emailPagador) {
        if (!configurado()) {
            throw new IllegalStateException(
                    "Mercado Pago não configurado. Defina MERCADOPAGO_ACCESS_TOKEN no servidor.");
        }

        String back = !backUrlSuccess.isBlank()
                ? backUrlSuccess
                : "https://somosloven.com/sucesso.html";

        // 1) Preferência: preapproval pending com external_reference (melhor para webhook)
        try {
            Map<String, Object> autoRecurring = new HashMap<>();
            autoRecurring.put("frequency", 1);
            autoRecurring.put("frequency_type", "months");
            autoRecurring.put("transaction_amount", valorMensal.doubleValue());
            autoRecurring.put("currency_id", "BRL");
            autoRecurring.put("end_date", Instant.now().plus(730, ChronoUnit.DAYS).toString());

            Map<String, Object> body = new HashMap<>();
            body.put("reason", "Site de Casamento (" + slug + ")");
            body.put("external_reference", "site:" + siteId);
            body.put("payer_email", emailPagador);
            body.put("auto_recurring", autoRecurring);
            body.put("back_url", back);
            body.put("status", "pending");
            return lerCheckout(postMp("/preapproval", body));
        } catch (IllegalStateException preapprovalFalhou) {
            // 2) Fallback estável (TEST- costuma dar 500 no /preapproval):
            //    cria/reusa plano e redireciona para o init_point do plano.
            try {
                return checkoutViaPlano(siteId, slug, back);
            } catch (IllegalStateException planoFalhou) {
                throw new IllegalStateException(
                        preapprovalFalhou.getMessage() + " | plano: " + planoFalhou.getMessage(),
                        planoFalhou);
            }
        }
    }

    /**
     * Checkout pela URL do plano (sem criar /preapproval).
     * O MP cria a assinatura quando o comprador conclui o pagamento.
     */
    private Map<String, String> checkoutViaPlano(Long siteId, String slug, String backUrl) {
        Map<String, String> plano = garantirPlanoMensalDetalhes(backUrl);
        String init = plano.get("init_point");
        if (init == null || init.isBlank()) {
            throw new IllegalStateException("Plano do Mercado Pago sem link de checkout.");
        }
        // Ajuda a identificar no painel do MP; o vínculo real do site é por e-mail/plano no webhook
        if (!init.contains("external_reference")) {
            String sep = init.contains("?") ? "&" : "?";
            init = init + sep + "external_reference=" + encodeQuery("site:" + siteId)
                    + "&reason=" + encodeQuery("Site de Casamento (" + slug + ")");
        }
        Map<String, String> out = new HashMap<>();
        out.put("id", "plan:" + plano.get("id"));
        out.put("plan_id", plano.get("id"));
        out.put("init_point", init);
        out.put("status", "pending");
        return out;
    }

    private Map<String, String> garantirPlanoMensalDetalhes(String backUrl) {
        String planId = garantirPlanoMensal(backUrl);
        try {
            JsonNode json = getJson("/preapproval_plan/{id}", planId);
            Map<String, String> out = new HashMap<>();
            out.put("id", planId);
            String sandbox = json.path("sandbox_init_point").asText("");
            String prod = json.path("init_point").asText("");
            out.put("init_point", escolherInitPoint(sandbox, prod, accessToken));
            if (out.get("init_point").isBlank()) {
                throw new IllegalStateException("Plano sem init_point: " + planId);
            }
            return out;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível ler o plano " + planId, e);
        }
    }

    private String garantirPlanoMensal(String backUrl) {
        if (preapprovalPlanIdCache != null && !preapprovalPlanIdCache.isBlank()) {
            return preapprovalPlanIdCache;
        }
        if (!preapprovalPlanIdConfigurado.isBlank()) {
            preapprovalPlanIdCache = preapprovalPlanIdConfigurado;
            return preapprovalPlanIdCache;
        }

        Map<String, Object> autoRecurring = new HashMap<>();
        autoRecurring.put("frequency", 1);
        autoRecurring.put("frequency_type", "months");
        autoRecurring.put("transaction_amount", valorMensal.doubleValue());
        autoRecurring.put("currency_id", "BRL");

        Map<String, Object> body = new HashMap<>();
        body.put("reason", "Site de Casamento — plano mensal");
        body.put("auto_recurring", autoRecurring);
        body.put("back_url", backUrl);

        String resposta = postMp("/preapproval_plan", body);
        try {
            String id = objectMapper.readTree(resposta).path("id").asText(null);
            if (id == null || id.isBlank()) {
                throw new IllegalStateException("Plano sem id: " + resposta);
            }
            preapprovalPlanIdCache = id;
            return id;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível criar o plano no Mercado Pago.", e);
        }
    }

    public JsonNode buscarPagamento(String paymentId) {
        return getJson("/v1/payments/{id}", paymentId);
    }

    public JsonNode buscarPreapproval(String preapprovalId) {
        return getJson("/preapproval/{id}", preapprovalId);
    }

    public JsonNode buscarAuthorizedPayment(String authorizedPaymentId) {
        return getJson("/authorized_payments/{id}", authorizedPaymentId);
    }

    private String postMp(String path, Object body) {
        return postMpComToken(path, body, accessToken);
    }

    private String postMpComToken(String path, Object body, String token) {
        try {
            return restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (org.springframework.web.client.RestClientResponseException e) {
            String detalhe = e.getResponseBodyAsString();
            if (detalhe == null || detalhe.isBlank()) detalhe = e.getMessage();
            throw new IllegalStateException("Mercado Pago recusou (" + path + "): " + detalhe, e);
        }
    }

    private JsonNode getJson(String uri, String id) {
        return getJsonComToken(uri, id, accessToken);
    }

    private JsonNode getJsonComToken(String uri, String id, String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Mercado Pago não configurado.");
        }
        try {
            String resposta = restClient.get()
                    .uri(uri, id)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(resposta);
        } catch (org.springframework.web.client.RestClientResponseException e) {
            throw new IllegalStateException(
                    "Falha ao buscar recurso MP " + id + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível ler recurso MP " + id, e);
        }
    }

    private Map<String, String> lerCheckout(String resposta) {
        try {
            JsonNode json = objectMapper.readTree(resposta);
            Map<String, String> out = new HashMap<>();
            out.put("id", json.path("id").asText());
            out.put("init_point", escolherInitPoint(json, accessToken));
            out.put("status", json.path("status").asText("pending"));
            if (out.get("id").isBlank()) {
                throw new IllegalStateException("Resposta do Mercado Pago sem id da assinatura.");
            }
            if (out.get("init_point").isBlank()) {
                throw new IllegalStateException("Resposta do Mercado Pago sem link de checkout.");
            }
            return out;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Resposta inválida do Mercado Pago ao criar assinatura.", e);
        }
    }

    /**
     * Com token TEST- prioriza sandbox_init_point; com APP- prioriza init_point de produção.
     */
    private static String escolherInitPoint(JsonNode json, String token) {
        String sandbox = json.path("sandbox_init_point").asText("");
        String prod = json.path("init_point").asText("");
        return escolherInitPoint(sandbox, prod, token);
    }

    private static String escolherInitPoint(String sandbox, String prod, String token) {
        boolean teste = token != null && token.startsWith("TEST-");
        if (teste) {
            // Preferência/Checkout Pro tem sandbox_init_point; Assinaturas NÃO.
            // Não reescrever o host — sandbox.mercadopago.../subscriptions dá 404.
            return !sandbox.isBlank() ? sandbox : prod;
        }
        return !prod.isBlank() ? prod : sandbox;
    }

    public static String encodeQuery(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
