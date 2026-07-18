package com.casamento.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Integração Mercado Pago:
 * - Checkout Pro: taxa de criação (R$ 99)
 * - Preapproval: mensalidade (R$ 49,90) a partir do 2º mês
 */
@Service
public class MercadoPagoService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String accessToken;
    private final String notificationUrl;
    private final String backUrlSuccess;
    private final String backUrlFailure;
    private final BigDecimal valorCriacao;
    private final BigDecimal valorMensal;
    private final String preapprovalPlanIdConfigurado;
    private volatile String preapprovalPlanIdCache;

    public MercadoPagoService(
            ObjectMapper objectMapper,
            @Value("${mercadopago.access-token:}") String accessToken,
            @Value("${mercadopago.notification-url:}") String notificationUrl,
            @Value("${mercadopago.back-url-success:}") String backUrlSuccess,
            @Value("${mercadopago.back-url-failure:}") String backUrlFailure,
            @Value("${mercadopago.valor-criacao:99.00}") BigDecimal valorCriacao,
            @Value("${mercadopago.valor-mensal:49.90}") BigDecimal valorMensal,
            @Value("${mercadopago.preapproval-plan-id:}") String preapprovalPlanId) {
        this.objectMapper = objectMapper;
        this.accessToken = accessToken == null ? "" : accessToken.trim();
        this.notificationUrl = notificationUrl == null ? "" : notificationUrl.trim();
        this.backUrlSuccess = backUrlSuccess == null ? "" : backUrlSuccess.trim();
        this.backUrlFailure = backUrlFailure == null ? "" : backUrlFailure.trim();
        this.valorCriacao = valorCriacao;
        this.valorMensal = valorMensal;
        this.preapprovalPlanIdConfigurado = preapprovalPlanId == null ? "" : preapprovalPlanId.trim();
        this.restClient = RestClient.builder()
                .baseUrl("https://api.mercadopago.com")
                .build();
    }

    public boolean configurado() {
        return !accessToken.isBlank();
    }

    public BigDecimal getValorCriacao() {
        return valorCriacao;
    }

    public BigDecimal getValorMensal() {
        return valorMensal;
    }

    /**
     * Preferência Checkout Pro — taxa de R$ 99 (criação + 1º mês).
     */
    public Map<String, String> criarPreferenciaCriacao(
            Long siteId,
            String slug,
            String emailPagador,
            String nomeNoiva,
            String nomeNoivo) {

        if (!configurado()) {
            throw new IllegalStateException(
                    "Mercado Pago não configurado. Defina MERCADOPAGO_ACCESS_TOKEN no servidor.");
        }

        Map<String, Object> item = new HashMap<>();
        item.put("title", "Site de Casamento — criação + 1º mês");
        item.put("description", nomeNoiva + " & " + nomeNoivo + " (" + slug + ")");
        item.put("quantity", 1);
        item.put("currency_id", "BRL");
        item.put("unit_price", valorCriacao.doubleValue());

        Map<String, Object> body = new HashMap<>();
        body.put("items", List.of(item));
        body.put("payer", Map.of("email", emailPagador));
        body.put("external_reference", "site:" + siteId);
        body.put("statement_descriptor", "SITE CASAMENTO");

        if (!backUrlSuccess.isBlank()) {
            Map<String, String> backUrls = new HashMap<>();
            backUrls.put("success", backUrlSuccess);
            backUrls.put("failure", backUrlFailure.isBlank() ? backUrlSuccess : backUrlFailure);
            backUrls.put("pending", backUrlSuccess);
            body.put("back_urls", backUrls);
            body.put("auto_return", "approved");
        }
        if (!notificationUrl.isBlank()) {
            body.put("notification_url", notificationUrl);
        }

        String resposta = restClient.post()
                .uri("/checkout/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .body(body)
                .retrieve()
                .body(String.class);

        return lerPreferenciaOuPreapproval(resposta, "preferência");
    }

    /**
     * Assinatura pending — mensalidade R$ 49,90 com free_trial no plano (1º mês já pago nos R$ 99).
     */
    public Map<String, String> criarPreapprovalMensal(
            Long siteId,
            String slug,
            String emailPagador,
            String nomeNoiva,
            String nomeNoivo,
            Instant inicioAssinatura) {

        if (!configurado()) {
            throw new IllegalStateException("Mercado Pago não configurado.");
        }

        String planId = garantirPlanoMensal();
        String back = !backUrlSuccess.isBlank()
                ? backUrlSuccess
                : "https://eukevytosdev.github.io/site-casamento-landing/sucesso.html";

        Map<String, Object> body = new HashMap<>();
        body.put("preapproval_plan_id", planId);
        body.put("reason", "Site de Casamento mensalidade (" + slug + ")");
        body.put("external_reference", "site:" + siteId);
        body.put("payer_email", emailPagador);
        body.put("back_url", back);
        body.put("status", "pending");

        try {
            String resposta = postMp("/preapproval", body);
            Map<String, String> out = lerPreferenciaOuPreapproval(resposta, "assinatura");
            try {
                JsonNode json = objectMapper.readTree(resposta);
                out.put("status", json.path("status").asText("pending"));
            } catch (Exception ignored) {
                out.putIfAbsent("status", "pending");
            }
            return out;
        } catch (IllegalStateException e) {
            // Fallback: assinatura sem plano (caso o plano falhe em algumas contas)
            return criarPreapprovalSemPlano(siteId, slug, emailPagador, back);
        }
    }

    private String garantirPlanoMensal() {
        if (preapprovalPlanIdCache != null && !preapprovalPlanIdCache.isBlank()) {
            return preapprovalPlanIdCache;
        }
        if (preapprovalPlanIdConfigurado != null && !preapprovalPlanIdConfigurado.isBlank()) {
            preapprovalPlanIdCache = preapprovalPlanIdConfigurado;
            return preapprovalPlanIdCache;
        }

        Map<String, Object> freeTrial = Map.of(
                "frequency", 1,
                "frequency_type", "months"
        );
        Map<String, Object> autoRecurring = new HashMap<>();
        autoRecurring.put("frequency", 1);
        autoRecurring.put("frequency_type", "months");
        autoRecurring.put("transaction_amount", valorMensal.doubleValue());
        autoRecurring.put("currency_id", "BRL");
        autoRecurring.put("free_trial", freeTrial);

        String back = !backUrlSuccess.isBlank()
                ? backUrlSuccess
                : "https://eukevytosdev.github.io/site-casamento-landing/sucesso.html";

        Map<String, Object> body = new HashMap<>();
        body.put("reason", "Site de Casamento — mensalidade");
        body.put("auto_recurring", autoRecurring);
        body.put("back_url", back);

        String resposta = postMp("/preapproval_plan", body);
        try {
            JsonNode json = objectMapper.readTree(resposta);
            String id = json.path("id").asText(null);
            if (id == null || id.isBlank()) {
                throw new IllegalStateException("Plano de assinatura sem id: " + resposta);
            }
            preapprovalPlanIdCache = id;
            return id;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível criar o plano de mensalidade no Mercado Pago.", e);
        }
    }

    private Map<String, String> criarPreapprovalSemPlano(
            Long siteId, String slug, String emailPagador, String back) {

        Map<String, Object> autoRecurring = new HashMap<>();
        autoRecurring.put("frequency", 1);
        autoRecurring.put("frequency_type", "months");
        autoRecurring.put("transaction_amount", valorMensal.doubleValue());
        autoRecurring.put("currency_id", "BRL");

        Map<String, Object> body = new HashMap<>();
        body.put("reason", "Site de Casamento mensalidade (" + slug + ")");
        body.put("external_reference", "site:" + siteId);
        body.put("payer_email", emailPagador);
        body.put("auto_recurring", autoRecurring);
        body.put("back_url", back);
        body.put("status", "pending");

        String resposta = postMp("/preapproval", body);
        Map<String, String> out = lerPreferenciaOuPreapproval(resposta, "assinatura");
        out.put("status", "pending");
        return out;
    }

    private String postMp(String path, Object body) {
        try {
            return restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (org.springframework.web.client.RestClientResponseException e) {
            String detalhe = e.getResponseBodyAsString();
            if (detalhe == null || detalhe.isBlank()) {
                detalhe = e.getMessage();
            }
            throw new IllegalStateException("Mercado Pago recusou (" + path + "): " + detalhe, e);
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

    private JsonNode getJson(String uri, String id) {
        if (!configurado()) {
            throw new IllegalStateException("Mercado Pago não configurado.");
        }
        String resposta = restClient.get()
                .uri(uri, id)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(String.class);
        try {
            return objectMapper.readTree(resposta);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível ler recurso MP " + id, e);
        }
    }

    private Map<String, String> lerPreferenciaOuPreapproval(String resposta, String rotulo) {
        try {
            JsonNode json = objectMapper.readTree(resposta);
            Map<String, String> out = new HashMap<>();
            out.put("id", json.path("id").asText());
            String init = json.path("init_point").asText();
            if (init == null || init.isBlank()) {
                init = json.path("sandbox_init_point").asText();
            }
            out.put("init_point", init);
            if (out.get("id") == null || out.get("id").isBlank()) {
                throw new IllegalStateException("Resposta do Mercado Pago sem id (" + rotulo + ").");
            }
            return out;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Resposta inválida do Mercado Pago ao criar " + rotulo + ".", e);
        }
    }
}
