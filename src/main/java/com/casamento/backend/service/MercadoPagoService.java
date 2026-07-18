package com.casamento.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Integração Mercado Pago — plano único recorrente (Assinaturas / preapproval).
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
    private volatile String preapprovalPlanIdCache;

    public MercadoPagoService(
            ObjectMapper objectMapper,
            @Value("${mercadopago.access-token:}") String accessToken,
            @Value("${mercadopago.notification-url:}") String notificationUrl,
            @Value("${mercadopago.back-url-success:}") String backUrlSuccess,
            @Value("${mercadopago.back-url-failure:}") String backUrlFailure,
            @Value("${mercadopago.valor-mensal:59.90}") BigDecimal valorMensal,
            @Value("${mercadopago.permanencia-minima-meses:6}") int permanenciaMinimaMeses,
            @Value("${mercadopago.preapproval-plan-id:}") String preapprovalPlanId) {
        this.objectMapper = objectMapper;
        this.accessToken = accessToken == null ? "" : accessToken.trim();
        this.notificationUrl = notificationUrl == null ? "" : notificationUrl.trim();
        this.backUrlSuccess = backUrlSuccess == null ? "" : backUrlSuccess.trim();
        this.backUrlFailure = backUrlFailure == null ? "" : backUrlFailure.trim();
        this.valorMensal = valorMensal;
        this.permanenciaMinimaMeses = permanenciaMinimaMeses;
        this.preapprovalPlanIdConfigurado = preapprovalPlanId == null ? "" : preapprovalPlanId.trim();
        this.restClient = RestClient.builder()
                .baseUrl("https://api.mercadopago.com")
                .build();
    }

    public boolean configurado() {
        return !accessToken.isBlank();
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

    /**
     * Cria assinatura pending (plano único mensal) e devolve init_point do Checkout.
     * Um único passo no cartão — cobrança recorrente R$ valorMensal.
     */
    public Map<String, String> criarAssinaturaMensal(
            Long siteId,
            String slug,
            String emailPagador) {

        if (!configurado()) {
            throw new IllegalStateException(
                    "Mercado Pago não configurado. Defina MERCADOPAGO_ACCESS_TOKEN no servidor.");
        }

        String back = !backUrlSuccess.isBlank()
                ? backUrlSuccess
                : "https://eukevytosdev.github.io/site-casamento-landing/sucesso.html";

        try {
            String planId = garantirPlanoMensal(back);
            Map<String, Object> body = new HashMap<>();
            body.put("preapproval_plan_id", planId);
            body.put("reason", "Site de Casamento (" + slug + ")");
            body.put("external_reference", "site:" + siteId);
            body.put("payer_email", emailPagador);
            body.put("back_url", back);
            body.put("status", "pending");
            return lerCheckout(postMp("/preapproval", body));
        } catch (IllegalStateException primeiro) {
            // Fallback: assinatura sem plano associado
            Map<String, Object> autoRecurring = new HashMap<>();
            autoRecurring.put("frequency", 1);
            autoRecurring.put("frequency_type", "months");
            autoRecurring.put("transaction_amount", valorMensal.doubleValue());
            autoRecurring.put("currency_id", "BRL");

            Map<String, Object> body = new HashMap<>();
            body.put("reason", "Site de Casamento (" + slug + ")");
            body.put("external_reference", "site:" + siteId);
            body.put("payer_email", emailPagador);
            body.put("auto_recurring", autoRecurring);
            body.put("back_url", back);
            body.put("status", "pending");

            try {
                return lerCheckout(postMp("/preapproval", body));
            } catch (IllegalStateException segundo) {
                throw new IllegalStateException(
                        primeiro.getMessage() + " | fallback: " + segundo.getMessage(), segundo);
            }
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

    private Map<String, String> lerCheckout(String resposta) {
        try {
            JsonNode json = objectMapper.readTree(resposta);
            Map<String, String> out = new HashMap<>();
            out.put("id", json.path("id").asText());
            String init = json.path("init_point").asText();
            if (init == null || init.isBlank()) {
                init = json.path("sandbox_init_point").asText();
            }
            out.put("init_point", init);
            out.put("status", json.path("status").asText("pending"));
            if (out.get("id") == null || out.get("id").isBlank()) {
                throw new IllegalStateException("Resposta do Mercado Pago sem id da assinatura.");
            }
            if (out.get("init_point") == null || out.get("init_point").isBlank()) {
                throw new IllegalStateException("Resposta do Mercado Pago sem link de checkout.");
            }
            return out;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Resposta inválida do Mercado Pago ao criar assinatura.", e);
        }
    }
}
