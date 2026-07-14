package com.casamento.backend.controller;

import com.casamento.backend.config.SiteContext;
import com.casamento.backend.model.PresencaCasamento;
import com.casamento.backend.model.Site;
import com.casamento.backend.repository.PresencaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/presenca")
@CrossOrigin(origins = "*")
public class AdminPresencaController {

    @Autowired
    private PresencaRepository presencaRepository;

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
        return ResponseEntity.ok(presencaRepository.findBySiteId(site.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(@PathVariable Long id) {
        Site site = siteAtual();
        if (site == null) {
            return semSite();
        }

        return presencaRepository.findById(id)
                .filter(p -> p.getSite() != null && site.getId().equals(p.getSite().getId()))
                .map(presenca -> {
                    presencaRepository.delete(presenca);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
