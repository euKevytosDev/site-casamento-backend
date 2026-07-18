package com.casamento.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * Valida o header x-signature das notificações do Mercado Pago.
 * @see <a href="https://www.mercadopago.com.br/developers/pt/docs/your-integrations/notifications/webhooks">Docs MP</a>
 */
@Service
public class MercadoPagoWebhookSignatureService {

    private final String secret;

    public MercadoPagoWebhookSignatureService(
            @Value("${mercadopago.webhook-secret:}") String secret) {
        this.secret = secret == null ? "" : secret.trim();
    }

    public boolean secretConfigurado() {
        return !secret.isBlank();
    }

    /**
     * @return true se a assinatura é válida, ou se o secret ainda não foi configurado (modo permissivo).
     */
    public boolean validar(String xSignature, String xRequestId, String dataId) {
        if (!secretConfigurado()) {
            return true;
        }
        if (xSignature == null || xSignature.isBlank()
                || dataId == null || dataId.isBlank()) {
            return false;
        }

        Map<String, String> parts = parseSignature(xSignature);
        String ts = parts.get("ts");
        String hash = parts.get("v1");
        if (ts == null || hash == null) {
            return false;
        }

        String requestId = xRequestId == null ? "" : xRequestId;
        String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            String expected = toHex(raw);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    hash.toLowerCase().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    private static Map<String, String> parseSignature(String xSignature) {
        Map<String, String> out = new HashMap<>();
        for (String part : xSignature.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                out.put(kv[0].trim(), kv[1].trim());
            }
        }
        return out;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
