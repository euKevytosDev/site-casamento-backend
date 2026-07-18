package com.casamento.backend.controller;

import com.casamento.backend.service.AssinaturaService;
import com.casamento.backend.service.MercadoPagoWebhookSignatureService;
import com.casamento.backend.service.PresenteService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@CrossOrigin(origins = "*")
public class MercadoPagoWebhookController {

    private final AssinaturaService assinaturaService;
    private final PresenteService presenteService;
    private final MercadoPagoWebhookSignatureService signatureService;
    private final ObjectMapper objectMapper;

    public MercadoPagoWebhookController(
            AssinaturaService assinaturaService,
            PresenteService presenteService,
            MercadoPagoWebhookSignatureService signatureService,
            ObjectMapper objectMapper) {
        this.assinaturaService = assinaturaService;
        this.presenteService = presenteService;
        this.signatureService = signatureService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/mercadopago")
    public ResponseEntity<String> receber(
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String id,
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestParam(name = "user_id", required = false) String userId,
            @RequestBody(required = false) String body) {

        try {
            String tipo = type != null ? type : topic;
            String recursoId = dataId != null ? dataId : id;
            String userHint = userId;

            if (body != null && !body.isBlank()) {
                JsonNode json = objectMapper.readTree(body);
                if (recursoId == null || recursoId.isBlank()) {
                    recursoId = json.path("data").path("id").asText(null);
                }
                if (tipo == null || tipo.isBlank()) {
                    tipo = json.path("type").asText(json.path("topic").asText(""));
                }
                if (userHint == null || userHint.isBlank()) {
                    userHint = json.path("user_id").asText(json.path("userId").asText(""));
                }
            }

            if (recursoId == null || recursoId.isBlank()) {
                return ResponseEntity.ok("ok");
            }

            if (!signatureService.validar(xSignature, xRequestId, recursoId)) {
                return ResponseEntity.status(401).body("assinatura inválida");
            }

            processar(tipo, recursoId, userHint);
        } catch (Exception ignored) {
        }

        return ResponseEntity.ok("ok");
    }

    /** Ping do MP / healthcheck — não processa pagamento (evita abuso via GET). */
    @GetMapping("/mercadopago")
    public ResponseEntity<String> ping(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok("ok");
    }

    private void processar(String tipo, String recursoId, String userHint) {
        String t = tipo == null ? "" : tipo.toLowerCase();

        if (t.isBlank() || "payment".equals(t)) {
            presenteService.processarPagamentoPresente(recursoId, userHint);
            assinaturaService.processarPagamentoAprovado(recursoId);
        } else if ("subscription_preapproval".equals(t)
                || "subscription".equals(t)
                || "preapproval".equals(t)) {
            assinaturaService.processarPreapproval(recursoId);
        } else if ("subscription_authorized_payment".equals(t)
                || "subscription_authorized".equals(t)
                || "authorized_payment".equals(t)) {
            assinaturaService.processarAuthorizedPayment(recursoId);
        }
    }
}
