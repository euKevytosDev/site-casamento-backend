package com.casamento.backend.controller;

import com.casamento.backend.config.SiteContext;
import com.casamento.backend.model.Site;
import com.casamento.backend.repository.HistoricoCompraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/historico-compras")
@CrossOrigin(origins = "*")
public class AdminHistoricoCompraController {

    @Autowired
    private HistoricoCompraRepository historicoCompraRepository;

    private Site siteAtual() {
        return SiteContext.get();
    }

    private ResponseEntity<?> semSite() {
        return ResponseEntity.badRequest()
                .body("Informe o header X-Site-Id com o slug do casamento (ex: rafaekevin).");
    }

    @GetMapping
    public ResponseEntity<?> listarTodos() {
        Site site = siteAtual();
        if (site == null) {
            return semSite();
        }
        return ResponseEntity.ok(
                historicoCompraRepository.findBySiteIdOrderByDataCompraDesc(site.getId())
        );
    }
}
