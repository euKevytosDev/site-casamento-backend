package com.casamento.backend.controller;

import com.casamento.backend.service.AssinaturaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Webhooks Asaas — liberação/bloqueio da assinatura SaaS.
 * Responde 200 na hora (Asaas corta a fila se demorar / falhar).
 */
@RestController
@RequestMapping("/api/webhooks")
@CrossOrigin(origins = "*")
public class AsaasWebhookController {

    private final AssinaturaService assinaturaService;
    private final ObjectMapper objectMapper;
    private final String webhookToken;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "asaas-webhook");
        t.setDaemon(true);
        return t;
    });

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

        boolean tokenOk = webhookToken.isBlank()
                || (asaasToken != null && webhookToken.equals(asaasToken.trim()));

        if (tokenOk && body != null && !body.isBlank()) {
            final String payload = body;
            executor.execute(() -> {
                try {
                    JsonNode json = objectMapper.readTree(payload);
                    assinaturaService.processarWebhookAsaas(json.path("event").asText(""), json);
                } catch (Exception ignored) {
                }
            });
        }

        return ResponseEntity.ok("ok");
    }

    @GetMapping("/asaas")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("ok");
    }
}
