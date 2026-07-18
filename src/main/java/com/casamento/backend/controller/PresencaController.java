package com.casamento.backend.controller;

import com.casamento.backend.config.SiteContext;
import com.casamento.backend.model.PresencaCasamento;
import com.casamento.backend.model.Site;
import com.casamento.backend.repository.PresencaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/presenca")
@CrossOrigin(origins = "*")
public class PresencaController {

    private static final int MAX_MEMBROS = 40;

    @Autowired
    private PresencaRepository presencaRepository;

    /** Site da request atual (vindo do header X-Site-Id via SiteFilter). */
    private Site siteAtual() {
        return SiteContext.get();
    }

    /**
     * Confirma família inteira.
     * Antes de salvar, amarra CADA convidado ao Site do header.
     * Não aceita id no body (evita sobrescrever RSVP de outro casamento).
     */
    @PostMapping("/confirmar-familia")
    public ResponseEntity<String> confirmarPresencaFamilia(@RequestBody List<Map<String, Object>> listaFamilia) {
        Site site = siteAtual();
        if (site == null) {
            return ResponseEntity.badRequest()
                    .body("Informe o header X-Site-Id com o slug do casamento (ex: rafaekevin).");
        }
        if (listaFamilia == null || listaFamilia.isEmpty()) {
            return ResponseEntity.badRequest().body("Informe ao menos um convidado.");
        }
        if (listaFamilia.size() > MAX_MEMBROS) {
            return ResponseEntity.badRequest().body("Limite de " + MAX_MEMBROS + " convidados por confirmação.");
        }

        List<PresencaCasamento> novos = new ArrayList<>();
        for (Map<String, Object> raw : listaFamilia) {
            PresencaCasamento membro = new PresencaCasamento();
            membro.setNomeConvidado(asString(raw.get("nomeConvidado")));
            membro.setIdade(asInt(raw.get("idade")));
            membro.setConfirmado(asBoolean(raw.get("confirmado"), true));
            if (membro.getNomeConvidado() == null || membro.getNomeConvidado().isBlank()) {
                return ResponseEntity.badRequest().body("Cada convidado precisa de nome.");
            }
            membro.setSite(site);
            novos.add(membro);
        }

        presencaRepository.saveAll(novos);
        return ResponseEntity.ok("Presença da família confirmada com sucesso!");
    }

    private static String asString(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    private static Integer asInt(Object v) {
        if (v == null) return null;
        try {
            return Integer.valueOf(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean asBoolean(Object v, boolean padrao) {
        if (v == null) return padrao;
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(v));
    }
}
