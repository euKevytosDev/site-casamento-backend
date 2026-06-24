package com.casamento.backend.controller;

import com.casamento.backend.model.PresencaCasamento;
import com.casamento.backend.repository.PresencaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/presenca")
@CrossOrigin(origins = "*")
public class AdminPresencaController {

    @Autowired
    private PresencaRepository presencaRepository;

    @GetMapping
    public List<PresencaCasamento> listarTodos() {
        return presencaRepository.findAll();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        if (!presencaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        presencaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
