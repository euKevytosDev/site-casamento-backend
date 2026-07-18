package com.casamento.backend.controller;

import com.casamento.backend.config.SiteContext;
import com.casamento.backend.dto.SiteConfigResponse;
import com.casamento.backend.model.Site;
import com.casamento.backend.service.SiteConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Config pública do casamento atual (header X-Site-Id).
 * O front do convite busca aqui pra montar textos, cores e fotos.
 */
@RestController
@RequestMapping("/api/site")
@CrossOrigin(origins = "*")
public class SitePublicController {

    private final SiteConfigService siteConfigService;

    public SitePublicController(SiteConfigService siteConfigService) {
        this.siteConfigService = siteConfigService;
    }

    @GetMapping("/config")
    public ResponseEntity<?> config() {
        Site site = SiteContext.get();
        if (site == null) {
            return ResponseEntity.badRequest()
                    .body("Informe o header X-Site-Id com o slug do casamento (ex: rafaekevin).");
        }
        SiteConfigResponse response = siteConfigService.toResponse(site);
        return ResponseEntity.ok(response);
    }
}
