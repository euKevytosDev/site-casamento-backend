package com.casamento.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Assinatura SaaS via Asaas (cliente + assinatura mensal + link da fatura).
 * Presentes no cartão continuam no Mercado Pago OAuth da noiva.
 */
@Service
public class AsaasService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String backUrlSuccess;
    private final BigDecimal valorMensal;
    private final int permanenciaMinimaMeses;
    private final int trialDias;

    public AsaasService(
            ObjectMapper objectMapper,
            @Value("${asaas.api-key:}") String apiKey,
            @Value("${asaas.api-url:https://api.asaas.com}") String apiUrl,
            @Value("${asaas.back-url-success:}") String backUrlSuccess,
            @Value("${asaas.valor-mensal:59.90}") BigDecimal valorMensal,
            @Value("${asaas.permanencia-minima-meses:0}") int permanenciaMinimaMeses,
            @Value("${asaas.trial-dias:14}") int trialDias) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.backUrlSuccess = backUrlSuccess == null ? "" : backUrlSuccess.trim();
        this.valorMensal = valorMensal;
        this.permanenciaMinimaMeses = permanenciaMinimaMeses;
        this.trialDias = Math.max(0, trialDias);
        String base = (apiUrl == null || apiUrl.isBlank()) ? "https://api.asaas.com" : apiUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        this.restClient = RestClient.builder()
                .baseUrl(base)
                .defaultHeader("User-Agent", "Loven-SiteCasamento/1.0")
                .build();
    }

    public boolean configurado() {
        return !apiKey.isBlank();
    }

    public boolean modoSandbox() {
        return apiKey.contains("_hml_") || apiKey.toLowerCase().contains("sandbox");
    }

    public BigDecimal getValorMensal() {
        return valorMensal;
    }

    public int getPermanenciaMinimaMeses() {
        return permanenciaMinimaMeses;
    }

    public int getTrialDias() {
        return trialDias;
    }

    /**
     * Cria (ou reusa) cliente + assinatura MONTHLY UNDEFINEDIFIED e devolve o link da 1ª fatura.
     * Com trial, a 1ª cobrança fica para daqui a {@link #trialDias} dias.
     *
     * @return id (subscription), customer_id, init_point (invoiceUrl), status, first_due_date
     */
    public Map<String, String> criarAssinaturaMensal(
            Long siteId,
            String slug,
            String nomeCliente,
            String email,
            String cpfCnpj,
            String subscriptionIdExistente) {
        return criarAssinaturaMensal(siteId, slug, nomeCliente, email, cpfCnpj, subscriptionIdExistente, false);
    }

    /**
     * @param cobrarNaHora se true, ignora trial e agenda a 1ª cobrança para hoje
     *                     (ex.: trial já usado / assinatura atrasada).
     */
    public Map<String, String> criarAssinaturaMensal(
            Long siteId,
            String slug,
            String nomeCliente,
            String email,
            String cpfCnpj,
            String subscriptionIdExistente,
            boolean cobrarNaHora) {

        if (!configurado()) {
            throw new IllegalStateException("Asaas não configurado (ASAAS_API_KEY).");
        }

        String cpfNorm = soDigitos(cpfCnpj);
        if (cpfNorm.length() != 11 && cpfNorm.length() != 14) {
            throw new IllegalArgumentException("Informe um CPF (11 dígitos) ou CNPJ (14 dígitos) válido.");
        }

        if (subscriptionIdExistente != null
                && !subscriptionIdExistente.isBlank()
                && subscriptionIdExistente.startsWith("sub_")) {
            String link = buscarInvoiceUrlAssinatura(subscriptionIdExistente);
            if (link != null && !link.isBlank()) {
                Map<String, String> reuse = new LinkedHashMap<>();
                reuse.put("id", subscriptionIdExistente);
                reuse.put("init_point", link);
                reuse.put("status", "ACTIVE");
                return reuse;
            }
        }

        String customerId = garantirCliente(nomeCliente, email, cpfNorm, siteId);

        boolean usarTrial = !cobrarNaHora && trialDias > 0;
        LocalDate primeiraCobranca = usarTrial
                ? LocalDate.now().plusDays(trialDias)
                : LocalDate.now();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("customer", customerId);
        // Cartão obrigatório: no trial a cobrança fica para o 15º dia; no mensal cobra na hora
        body.put("billingType", "CREDIT_CARD");
        body.put("value", valorMensal);
        body.put("nextDueDate", primeiraCobranca.toString());
        body.put("cycle", "MONTHLY");
        body.put("description", usarTrial
                ? ("Loven — trial " + trialDias + " dias (" + slug + ")")
                : ("Loven — site de casamento (" + slug + ")"));
        body.put("externalReference", "site:" + siteId);
        if (!backUrlSuccess.isBlank()) {
            Map<String, Object> callback = new LinkedHashMap<>();
            callback.put("successUrl", backUrlSuccess);
            callback.put("autoRedirect", true);
            body.put("callback", callback);
        }

        JsonNode sub = postJson("/v3/subscriptions", body);
        String subId = sub.path("id").asText("");
        if (subId.isBlank()) {
            throw new IllegalStateException("Asaas não retornou id da assinatura.");
        }

        String invoiceUrl = buscarInvoiceUrlAssinatura(subId);
        if (invoiceUrl == null || invoiceUrl.isBlank()) {
            throw new IllegalStateException("Assinatura criada, mas o link de pagamento ainda não está disponível. Tente de novo em instantes.");
        }

        Map<String, String> out = new LinkedHashMap<>();
        out.put("id", subId);
        out.put("customer_id", customerId);
        out.put("init_point", invoiceUrl);
        out.put("status", sub.path("status").asText("ACTIVE"));
        out.put("first_due_date", primeiraCobranca.toString());
        return out;
    }

    public JsonNode buscarPagamento(String paymentId) {
        return getJson("/v3/payments/" + paymentId);
    }

    public JsonNode buscarAssinatura(String subscriptionId) {
        return getJson("/v3/subscriptions/" + subscriptionId);
    }

    private String garantirCliente(String nome, String email, String cpfNorm, Long siteId) {
        JsonNode existentes = getJson("/v3/customers?cpfCnpj=" + cpfNorm + "&limit=1");
        JsonNode data = existentes.path("data");
        if (data.isArray() && !data.isEmpty()) {
            String id = data.get(0).path("id").asText("");
            if (!id.isBlank()) {
                return id;
            }
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", nome == null || nome.isBlank() ? "Casal Loven" : nome.trim());
        body.put("cpfCnpj", cpfNorm);
        if (email != null && !email.isBlank()) {
            body.put("email", email.trim().toLowerCase());
        }
        body.put("externalReference", "site:" + siteId);
        body.put("notificationDisabled", false);

        JsonNode created = postJson("/v3/customers", body);
        String id = created.path("id").asText("");
        if (id.isBlank()) {
            throw new IllegalStateException("Asaas não retornou id do cliente.");
        }
        return id;
    }

    private String buscarInvoiceUrlAssinatura(String subscriptionId) {
        JsonNode lista = getJson("/v3/subscriptions/" + subscriptionId + "/payments?limit=10");
        JsonNode data = lista.path("data");
        if (!data.isArray()) {
            return null;
        }
        for (JsonNode p : data) {
            String status = p.path("status").asText("");
            if ("PENDING".equalsIgnoreCase(status)
                    || "OVERDUE".equalsIgnoreCase(status)
                    || "AWAITING_PAYMENT".equalsIgnoreCase(status)) {
                String url = p.path("invoiceUrl").asText("");
                if (!url.isBlank()) {
                    return url;
                }
            }
        }
        for (JsonNode p : data) {
            String url = p.path("invoiceUrl").asText("");
            if (!url.isBlank()) {
                return url;
            }
        }
        return null;
    }

    private JsonNode getJson(String path) {
        try {
            String raw = restClient.get()
                    .uri(path)
                    .header("access_token", apiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(raw == null ? "{}" : raw);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(mensagemErroAsaas(e), e);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao consultar Asaas: " + e.getMessage(), e);
        }
    }

    private JsonNode postJson(String path, Object body) {
        try {
            String raw = restClient.post()
                    .uri(path)
                    .header("access_token", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(raw == null ? "{}" : raw);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(mensagemErroAsaas(e), e);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao chamar Asaas: " + e.getMessage(), e);
        }
    }

    private String mensagemErroAsaas(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        try {
            JsonNode json = objectMapper.readTree(body == null ? "{}" : body);
            JsonNode errors = json.path("errors");
            if (errors.isArray() && !errors.isEmpty()) {
                String desc = errors.get(0).path("description").asText("");
                if (!desc.isBlank()) {
                    return desc;
                }
            }
        } catch (Exception ignored) {
        }
        return "Asaas HTTP " + e.getStatusCode().value()
                + (body == null || body.isBlank() ? "" : ": " + body);
    }

    private static String soDigitos(String v) {
        if (v == null) return "";
        return v.replaceAll("\\D", "");
    }
}
