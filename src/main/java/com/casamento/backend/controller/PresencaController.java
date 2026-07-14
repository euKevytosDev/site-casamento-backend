package com.casamento.backend.controller;

import com.casamento.backend.config.SiteContext;
import com.casamento.backend.model.PresencaCasamento;
import com.casamento.backend.model.Site;
import com.casamento.backend.repository.PresencaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/presenca")
@CrossOrigin(origins = "*")
public class PresencaController {

    @Autowired
    private PresencaRepository presencaRepository;

    /** Site da request atual (vindo do header X-Site-Id via SiteFilter). */
    private Site siteAtual() {
        return SiteContext.get();
    }

    /**
     * Confirma família inteira.
     * Antes de salvar, amarra CADA convidado ao Site do header.
     */
    @PostMapping("/confirmar-familia")
    public ResponseEntity<String> confirmarPresencaFamilia(@RequestBody List<PresencaCasamento> listaFamilia) {
        Site site = siteAtual();
        if (site == null) {
            return ResponseEntity.badRequest()
                    .body("Informe o header X-Site-Id com o slug do casamento (ex: rafaekevin).");
        }

        // Cada presença “pertence” a este casamento
        for (PresencaCasamento membro : listaFamilia) {
            membro.setSite(site);
        }

        presencaRepository.saveAll(listaFamilia);
        return ResponseEntity.ok("Presença da família confirmada com sucesso!");
    }

    /**
     * Lista só as presenças DESTE casamento (não misturam mais).
     */
    @GetMapping
    public ResponseEntity<?> listarTodos() {
        Site site = siteAtual();
        if (site == null) {
            return ResponseEntity.badRequest()
                    .body("Informe o header X-Site-Id com o slug do casamento (ex: rafaekevin).");
        }
        return ResponseEntity.ok(presencaRepository.findBySiteId(site.getId()));
    }
}
