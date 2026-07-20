package com.casamento.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Ping leve para UptimeRobot: acorda o Render sem consultar o banco.
 * Não envie X-Site-Id — assim o SiteFilter não abre conexão com o Neon.
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "db", "skipped"
        ));
    }
}
