package com.casamento.backend.controller;

import com.casamento.backend.config.SiteContext;
import com.casamento.backend.model.RecadoCasamento;
import com.casamento.backend.model.Site;
import com.casamento.backend.repository.RecadoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/recados")
@CrossOrigin(origins = "*")
public class AdminRecadoController {

    private final RecadoRepository recadoRepository;

    public AdminRecadoController(RecadoRepository recadoRepository) {
        this.recadoRepository = recadoRepository;
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        Site site = SiteContext.get();
        if (site == null) {
            return ResponseEntity.badRequest()
                    .body("Informe o header X-Site-Id com o slug do casamento.");
        }
        List<Map<String, Object>> lista = recadoRepository
                .findBySiteIdOrderByDataCadastroDesc(site.getId())
                .stream()
                .map(this::resumo)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(@PathVariable Long id) {
        Site site = SiteContext.get();
        if (site == null) {
            return ResponseEntity.badRequest()
                    .body("Informe o header X-Site-Id com o slug do casamento.");
        }
        Optional<RecadoCasamento> encontrado = recadoRepository.findById(id)
                .filter(r -> r.getSite() != null && site.getId().equals(r.getSite().getId()));
        if (encontrado.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        recadoRepository.delete(encontrado.get());
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> resumo(RecadoCasamento r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("nome", r.getNomeAutor());
        m.put("mensagem", r.getMensagem());
        m.put("dataCadastro", r.getDataCadastro() != null ? r.getDataCadastro().toString() : null);
        return m;
    }
}
