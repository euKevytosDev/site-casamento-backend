package com.casamento.backend.controller;

import com.casamento.backend.service.AssinaturaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Webhooks do Mercado Pago (pagamento da taxa de criação).
 * Configure notification_url apontando para /api/webhooks/mercadopago
 */
@RestController
@RequestMapping("/api/webhooks")
@CrossOrigin(origins = "*")
public class MercadoPagoWebhookController {

    private final AssinaturaService assinaturaService;
    private final ObjectMapper objectMapper;

    public MercadoPagoWebhookController(AssinaturaService assinaturaService, ObjectMapper objectMapper) {
        this.assinaturaService = assinaturaService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/mercadopago")
    public ResponseEntity<String> receber(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String id,
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestBody(required = false) String body) {

        try {
            String tipo = type != null ? type : topic;
            String pagamentoId = dataId != null ? dataId : id;

            if ((pagamentoId == null || pagamentoId.isBlank()) && body != null && !body.isBlank()) {
                JsonNode json = objectMapper.readTree(body);
                if (pagamentoId == null || pagamentoId.isBlank()) {
                    pagamentoId = json.path("data").path("id").asText(null);
                }
                if (tipo == null || tipo.isBlank()) {
                    tipo = json.path("type").asText(json.path("topic").asText(""));
                }
            }

            if (pagamentoId != null && !pagamentoId.isBlank()
                    && (tipo == null || tipo.isBlank() || "payment".equalsIgnoreCase(tipo))) {
                assinaturaService.processarPagamentoAprovado(pagamentoId);
            }
        } catch (Exception ignored) {
            // Sempre 200 para o MP não reenviar em loop agressivo por erro nosso
        }

        return ResponseEntity.ok("ok");
    }

    @GetMapping("/mercadopago")
    public ResponseEntity<String> ping(@RequestParam Map<String, String> params) {
        // Alguns ambientes fazem GET de verificação
        String topic = params.getOrDefault("topic", params.get("type"));
        String id = params.getOrDefault("data.id", params.get("id"));
        if (id != null && (topic == null || "payment".equalsIgnoreCase(topic))) {
            try {
                assinaturaService.processarPagamentoAprovado(id);
            } catch (Exception ignored) {
            }
        }
        return ResponseEntity.ok("ok");
    }
}
