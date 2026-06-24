package com.casamento.backend.controller;

import com.casamento.backend.model.PresenteCasamento;
import com.casamento.backend.repository.PresenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/presentes")
@CrossOrigin(origins = "*")
public class PresenteController {

    @Autowired
    private PresenteRepository presenteRepository;

    @GetMapping
    public List<PresenteCasamento> listarTodos() {
        return presenteRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PresenteCasamento> buscarPorId(@PathVariable Long id) {
        return presenteRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/comprar")
    public ResponseEntity<?> comprar(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        return presenteRepository.findById(id)
                .map(presente -> {
                    if (Boolean.TRUE.equals(presente.getComprado())) {
                        return ResponseEntity.status(409)
                                .body("Este presente já foi comprado.");
                    }
                    String nomeComprador = body.get("nomeComprador");
                    if (nomeComprador == null || nomeComprador.isBlank()) {
                        return ResponseEntity.badRequest()
                                .body("Informe o nome do comprador.");
                    }
                    presente.setComprado(true);
                    presente.setNomeComprador(nomeComprador.trim());
                    return ResponseEntity.ok(presenteRepository.save(presente));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
