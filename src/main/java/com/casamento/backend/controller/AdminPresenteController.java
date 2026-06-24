package com.casamento.backend.controller;

import com.casamento.backend.model.PresenteCasamento;
import com.casamento.backend.repository.PresenteRepository;
import com.casamento.backend.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/presentes")
@CrossOrigin(origins = "*")
public class AdminPresenteController {

    @Autowired
    private PresenteRepository presenteRepository;

    @Autowired
    private FileStorageService fileStorageService;

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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> criar(
            @RequestParam String nome,
            @RequestParam(required = false) String descricao,
            @RequestParam BigDecimal valor,
            @RequestParam("imagem") MultipartFile imagem) {

        try {
            PresenteCasamento presente = new PresenteCasamento();
            presente.setNome(nome);
            presente.setDescricao(descricao);
            presente.setValor(valor);
            presente.setImagem(fileStorageService.salvarImagem(imagem));
            presente.setComprado(false);
            presente.setNomeComprador(null);

            return ResponseEntity.status(HttpStatus.CREATED).body(presenteRepository.save(presente));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao salvar a imagem.");
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> atualizar(
            @PathVariable Long id,
            @RequestParam String nome,
            @RequestParam(required = false) String descricao,
            @RequestParam BigDecimal valor,
            @RequestParam(value = "imagem", required = false) MultipartFile imagem) {

        return presenteRepository.findById(id)
                .map(presente -> {
                    try {
                        presente.setNome(nome);
                        presente.setDescricao(descricao);
                        presente.setValor(valor);

                        if (imagem != null && !imagem.isEmpty()) {
                            fileStorageService.excluirImagem(presente.getImagem());
                            presente.setImagem(fileStorageService.salvarImagem(imagem));
                        }

                        return ResponseEntity.ok(presenteRepository.save(presente));
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(e.getMessage());
                    } catch (IOException e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao salvar a imagem.");
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/liberar")
    public ResponseEntity<?> liberar(@PathVariable Long id) {
        return presenteRepository.findById(id)
                .map(presente -> {
                    presente.setComprado(false);
                    presente.setNomeComprador(null);
                    return ResponseEntity.ok(presenteRepository.save(presente));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        return presenteRepository.findById(id)
                .map(presente -> {
                    fileStorageService.excluirImagem(presente.getImagem());
                    presenteRepository.delete(presente);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
