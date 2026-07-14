package com.casamento.backend.controller;

import com.casamento.backend.config.SiteContext;
import com.casamento.backend.dto.CompraCarrinhoRequest;
import com.casamento.backend.dto.FinalizarCarrinhoResponse;
import com.casamento.backend.dto.GerarPixResponse;
import com.casamento.backend.model.PresenteCasamento;
import com.casamento.backend.model.Site;
import com.casamento.backend.repository.PresenteRepository;
import com.casamento.backend.service.PresenteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/presentes")
@CrossOrigin(origins = "*")
public class PresenteController {

    @Autowired
    private PresenteRepository presenteRepository;

    @Autowired
    private PresenteService presenteService;

    /**
     * Pega o Site que o SiteFilter guardou a partir do header X-Site-Id.
     * Se não veio header / slug inválido → null.
     */
    private Site siteAtual() {
        return SiteContext.get();
    }

    @GetMapping
    public ResponseEntity<?> listarTodos() {
        Site site = siteAtual();
        // Sem site identificado, não devolvemos a lista global (evitar misturar casamentos)
        if (site == null) {
            return ResponseEntity.badRequest()
                    .body("Informe o header X-Site-Id com o slug do casamento (ex: rafaekevin).");
        }
        // Só presentes DESTE casamento
        return ResponseEntity.ok(presenteRepository.findBySiteId(site.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        Site site = siteAtual();
        if (site == null) {
            return ResponseEntity.badRequest()
                    .body("Informe o header X-Site-Id com o slug do casamento.");
        }

        return presenteRepository.findById(id)
                // Só devolve se o presente pertencer ao site do header
                .filter(p -> p.getSite() != null && site.getId().equals(p.getSite().getId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/gerar-pix")
    public ResponseEntity<?> gerarPix(@RequestBody CompraCarrinhoRequest request) {
        try {
            GerarPixResponse resposta = presenteService.gerarPix(request);
            return ResponseEntity.ok(resposta);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/finalizar-carrinho")
    public ResponseEntity<?> finalizarCarrinho(@RequestBody CompraCarrinhoRequest request) {
        try {
            FinalizarCarrinhoResponse resposta = presenteService.finalizarCarrinho(request);
            return ResponseEntity.ok(resposta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/comprar")
    public ResponseEntity<?> comprar(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        Site site = siteAtual();
        if (site == null) {
            return ResponseEntity.badRequest()
                    .body("Informe o header X-Site-Id com o slug do casamento.");
        }

        return presenteRepository.findById(id)
                .filter(p -> p.getSite() != null && site.getId().equals(p.getSite().getId()))
                .map(presente -> {
                    if (presente.getCotasDisponiveis() <= 0) {
                        return ResponseEntity.status(409)
                                .body("Não há mais cotas disponíveis para este item.");
                    }
                    String nomeComprador = body.get("nomeComprador");
                    if (nomeComprador == null || nomeComprador.isBlank()) {
                        return ResponseEntity.badRequest()
                                .body("Informe o nome do comprador.");
                    }
                    presente.setCotasVendidas(presente.getCotasVendidas() + 1);
                    presente.atualizarStatusComprado();
                    presente.setNomeComprador(nomeComprador.trim());
                    return ResponseEntity.ok(presenteRepository.save(presente));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
