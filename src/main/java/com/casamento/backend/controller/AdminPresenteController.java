package com.casamento.backend.controller;

import com.casamento.backend.config.SiteContext;
import com.casamento.backend.config.SiteLimites;
import com.casamento.backend.model.PresenteCasamento;
import com.casamento.backend.model.Site;
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

@RestController
@RequestMapping("/api/admin/presentes")
@CrossOrigin(origins = "*")
public class AdminPresenteController {

    @Autowired
    private PresenteRepository presenteRepository;

    @Autowired
    private FileStorageService fileStorageService;

    private Site siteAtual() {
        return SiteContext.get();
    }

    private ResponseEntity<?> semSite() {
        return ResponseEntity.badRequest()
                .body("Informe o header X-Site-Id com o slug do casamento (ex: rafaekevin).");
    }

    private boolean pertenceAoSite(PresenteCasamento presente, Site site) {
        return presente.getSite() != null && site.getId().equals(presente.getSite().getId());
    }

    @GetMapping
    public ResponseEntity<?> listarTodos() {
        Site site = siteAtual();
        if (site == null) {
            return semSite();
        }
        return ResponseEntity.ok(presenteRepository.findBySiteId(site.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        Site site = siteAtual();
        if (site == null) {
            return semSite();
        }

        return presenteRepository.findById(id)
                .filter(p -> pertenceAoSite(p, site))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> criar(
            @RequestParam String nome,
            @RequestParam(required = false) String descricao,
            @RequestParam BigDecimal valor,
            @RequestParam(defaultValue = "10") Integer cotasTotal,
            @RequestParam("imagem") MultipartFile imagem) {

        Site site = siteAtual();
        if (site == null) {
            return semSite();
        }

        if (!SiteLimites.semLimitePresentes(site)) {
            long total = presenteRepository.countBySiteId(site.getId());
            if (total >= SiteLimites.MAX_PRESENTES_CLIENTE) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Limite de " + SiteLimites.MAX_PRESENTES_CLIENTE
                                + " presentes atingido neste site.");
            }
        }

        try {
            PresenteCasamento presente = new PresenteCasamento();
            presente.setSite(site);
            presente.setNome(nome);
            presente.setDescricao(descricao);
            presente.setValor(valor);
            presente.setCotasTotal(cotasTotal);
            presente.setCotasVendidas(0);
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
            @RequestParam Integer cotasTotal,
            @RequestParam(value = "imagem", required = false) MultipartFile imagem) {

        Site site = siteAtual();
        if (site == null) {
            return semSite();
        }

        return presenteRepository.findById(id)
                .filter(p -> pertenceAoSite(p, site))
                .map(presente -> {
                    try {
                        presente.setNome(nome);
                        presente.setDescricao(descricao);
                        presente.setValor(valor);
                        presente.setCotasTotal(cotasTotal);
                        presente.atualizarStatusComprado();

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
        Site site = siteAtual();
        if (site == null) {
            return semSite();
        }

        return presenteRepository.findById(id)
                .filter(p -> pertenceAoSite(p, site))
                .map(presente -> {
                    presente.setComprado(false);
                    presente.setCotasVendidas(0);
                    presente.setNomeComprador(null);
                    return ResponseEntity.ok(presenteRepository.save(presente));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(@PathVariable Long id) {
        Site site = siteAtual();
        if (site == null) {
            return semSite();
        }

        return presenteRepository.findById(id)
                .filter(p -> pertenceAoSite(p, site))
                .map(presente -> {
                    fileStorageService.excluirImagem(presente.getImagem());
                    presenteRepository.delete(presente);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
