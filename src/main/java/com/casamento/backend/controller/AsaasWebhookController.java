package com.casamento.backend.controller;

import com.casamento.backend.service.AssinaturaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Webhooks Asaas — liberação/bloqueio da assinatura SaaS.
 * Configure no painel Asaas a URL:
 *   POST https://…/api/webhooks/asaas
 * e o token de autenticação (enviado no header asaas-access-token).
 */
@RestController
@RequestMapping("/api/webhooks")
@CrossOrigin(origins = "*")
public class AsaasWebhookController {

    private final AssinaturaService assinaturaService;
    private final ObjectMapper objectMapper;
    private final String webhookToken;

    public AsaasWebhookController(
            AssinaturaService assinaturaService,
            ObjectMapper objectMapper,
            @Value("${asaas.webhook-token:}") String webhookToken) {
        this.assinaturaService = assinaturaService;
        this.objectMapper = objectMapper;
        this.webhookToken = webhookToken == null ? "" : webhookToken.trim();
    }

    @PostMapping("/asaas")
    public ResponseEntity<String> receber(
            @RequestHeader(value = "asaas-access-token", required = false) String asaasToken,
            @RequestBody(required = false) String body) {

        if (!webhookToken.isBlank()) {
            if (asaasToken == null || !webhookToken.equals(asaasToken.trim())) {
                return ResponseEntity.status(401).body("token inválido");
            }
        }

        try {
            if (body != null && !body.isBlank()) {
                JsonNode json = objectMapper.readTree(body);
                String event = json.path("event").asText("");
                assinaturaService.processarWebhookAsaas(event, json);
            }
        } catch (Exception ignored) {
        }

        return ResponseEntity.ok("ok");
    }

    @GetMapping("/asaas")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("ok");
    }
}
