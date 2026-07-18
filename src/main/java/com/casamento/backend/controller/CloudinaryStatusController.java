package com.casamento.backend.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Diagnóstico do Cloudinary (sem expor segredos).
 * GET /api/admin/cloudinary-status — precisa estar logado.
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class CloudinaryStatusController {

    private final Cloudinary cloudinary;
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    public CloudinaryStatusController(
            Cloudinary cloudinary,
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        this.cloudinary = cloudinary;
        this.cloudName = cloudName == null ? "" : cloudName.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.apiSecret = apiSecret == null ? "" : apiSecret.trim();
    }

    @GetMapping("/cloudinary-status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("cloudName", cloudName.isBlank() ? null : cloudName);
        out.put("cloudNameOk", !cloudName.isBlank());
        out.put("apiKeyInformada", !apiKey.isBlank());
        out.put("apiKeyTamanho", apiKey.length());
        out.put("apiKeyPrefixo", apiKey.length() >= 4 ? apiKey.substring(0, 4) + "…" : null);
        out.put("apiSecretInformada", !apiSecret.isBlank());
        out.put("apiSecretTamanho", apiSecret.length());

        boolean pingOk = false;
        String pingErro = null;
        try {
            cloudinary.api().ping(ObjectUtils.emptyMap());
            pingOk = true;
        } catch (Exception e) {
            Throwable c = e;
            while (c.getCause() != null && c.getCause() != c) {
                c = c.getCause();
            }
            pingErro = c.getMessage() != null ? c.getMessage() : e.getClass().getSimpleName();
        }
        out.put("pingOk", pingOk);
        out.put("pingErro", pingErro);
        return ResponseEntity.ok(out);
    }
}
