package com.casamento.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Integração Mercado Pago — checkout da taxa de criação (R$ 99).
 * Access token via MERCADOPAGO_ACCESS_TOKEN no Render.
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

    public MercadoPagoService(
            ObjectMapper objectMapper,
            @Value("${mercadopago.access-token:}") String accessToken,
            @Value("${mercadopago.notification-url:}") String notificationUrl,
            @Value("${mercadopago.back-url-success:}") String backUrlSuccess,
            @Value("${mercadopago.back-url-failure:}") String backUrlFailure,
            @Value("${mercadopago.valor-criacao:99.00}") BigDecimal valorCriacao) {
        this.objectMapper = objectMapper;
        this.accessToken = accessToken == null ? "" : accessToken.trim();
        this.notificationUrl = notificationUrl == null ? "" : notificationUrl.trim();
        this.backUrlSuccess = backUrlSuccess == null ? "" : backUrlSuccess.trim();
        this.backUrlFailure = backUrlFailure == null ? "" : backUrlFailure.trim();
        this.valorCriacao = valorCriacao;
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

    /**
     * Cria preferência de pagamento (Checkout Pro) para a taxa de R$ 99.
     * @return mapa com id e init_point (URL do checkout)
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

        try {
            JsonNode json = objectMapper.readTree(resposta);
            Map<String, String> out = new HashMap<>();
            out.put("id", json.path("id").asText());
            String init = json.path("init_point").asText();
            if (init == null || init.isBlank()) {
                init = json.path("sandbox_init_point").asText();
            }
            out.put("init_point", init);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Resposta inválida do Mercado Pago ao criar preferência.", e);
        }
    }

    public JsonNode buscarPagamento(String paymentId) {
        if (!configurado()) {
            throw new IllegalStateException("Mercado Pago não configurado.");
        }
        String resposta = restClient.get()
                .uri("/v1/payments/{id}", paymentId)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(String.class);
        try {
            return objectMapper.readTree(resposta);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível ler o pagamento " + paymentId, e);
        }
    }
}
