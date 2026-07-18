package com.casamento.backend.controller;

import com.casamento.backend.config.SiteContext;
import com.casamento.backend.dto.FinalizarCarrinhoResponse;
import com.casamento.backend.model.PedidoPresente;
import com.casamento.backend.model.Site;
import com.casamento.backend.repository.PedidoPresenteRepository;
import com.casamento.backend.service.PresenteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pedidos PIX aguardando confirmação manual da noiva (cotas só baixam após confirmar).
 */
@RestController
@RequestMapping("/api/admin/pedidos-presente")
@CrossOrigin(origins = "*")
public class AdminPedidoPresenteController {

    private final PedidoPresenteRepository pedidoPresenteRepository;
    private final PresenteService presenteService;

    public AdminPedidoPresenteController(
            PedidoPresenteRepository pedidoPresenteRepository,
            PresenteService presenteService) {
        this.pedidoPresenteRepository = pedidoPresenteRepository;
        this.presenteService = presenteService;
    }

    @GetMapping("/pendentes-pix")
    public ResponseEntity<?> listarPendentesPix() {
        Site site = SiteContext.get();
        if (site == null) {
            return ResponseEntity.badRequest()
                    .body("Informe o header X-Site-Id com o slug do casamento.");
        }
        List<Map<String, Object>> lista = pedidoPresenteRepository
                .findBySiteIdAndStatusOrderByDataCriacaoDesc(site.getId(), "AGUARDANDO_PIX")
                .stream()
                .map(this::resumo)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/{id}/confirmar-pix")
    public ResponseEntity<?> confirmarPix(@PathVariable Long id) {
        try {
            FinalizarCarrinhoResponse resp = presenteService.confirmarPixManual(id);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/recusar-pix")
    public ResponseEntity<?> recusarPix(@PathVariable Long id) {
        try {
            presenteService.recusarPixManual(id);
            return ResponseEntity.ok(Map.of("mensagem", "Aviso de PIX marcado como recusado."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private Map<String, Object> resumo(PedidoPresente p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("nomeComprador", p.getNomeComprador());
        m.put("total", p.getTotal());
        m.put("itensJson", p.getItensJson());
        m.put("status", p.getStatus());
        m.put("dataCriacao", p.getDataCriacao() != null ? p.getDataCriacao().toString() : null);
        return m;
    }
}
