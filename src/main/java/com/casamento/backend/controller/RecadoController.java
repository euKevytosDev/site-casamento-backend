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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recados")
@CrossOrigin(origins = "*")
public class RecadoController {

    private static final int MAX_NOME = 80;
    private static final int MAX_MENSAGEM = 280;
    private static final int MAX_POR_SITE = 200;

    private final RecadoRepository recadoRepository;

    public RecadoController(RecadoRepository recadoRepository) {
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
                .map(this::publico)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody Map<String, String> body) {
        Site site = SiteContext.get();
        if (site == null) {
            return ResponseEntity.badRequest()
                    .body("Informe o header X-Site-Id com o slug do casamento.");
        }

        String nome = body.get("nome") == null ? "" : body.get("nome").trim();
        String mensagem = body.get("mensagem") == null ? "" : body.get("mensagem").trim();

        if (nome.isBlank()) {
            return ResponseEntity.badRequest().body("Informe seu nome.");
        }
        if (mensagem.isBlank()) {
            return ResponseEntity.badRequest().body("Escreva uma mensagem.");
        }
        if (nome.length() > MAX_NOME) {
            return ResponseEntity.badRequest().body("Nome: no máximo " + MAX_NOME + " caracteres.");
        }
        if (mensagem.length() > MAX_MENSAGEM) {
            return ResponseEntity.badRequest().body("Mensagem: no máximo " + MAX_MENSAGEM + " caracteres.");
        }
        if (recadoRepository.countBySiteId(site.getId()) >= MAX_POR_SITE) {
            return ResponseEntity.status(429).body("Mural cheio por enquanto. Tente mais tarde.");
        }

        RecadoCasamento recado = new RecadoCasamento();
        recado.setSite(site);
        recado.setNomeAutor(nome);
        recado.setMensagem(mensagem);
        recado = recadoRepository.save(recado);

        return ResponseEntity.ok(publico(recado));
    }

    private Map<String, Object> publico(RecadoCasamento r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("nome", r.getNomeAutor());
        m.put("mensagem", r.getMensagem());
        m.put("dataCadastro", r.getDataCadastro() != null ? r.getDataCadastro().toString() : null);
        return m;
    }
}
