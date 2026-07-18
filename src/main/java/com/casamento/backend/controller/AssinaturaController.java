package com.casamento.backend.controller;

import com.casamento.backend.service.AssinaturaService;
import com.casamento.backend.service.MercadoPagoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/assinatura")
@CrossOrigin(origins = "*")
public class AssinaturaController {

    private final AssinaturaService assinaturaService;
    private final MercadoPagoService mercadoPagoService;

    public AssinaturaController(AssinaturaService assinaturaService, MercadoPagoService mercadoPagoService) {
        this.assinaturaService = assinaturaService;
        this.mercadoPagoService = mercadoPagoService;
    }

    @GetMapping("/plano")
    public Map<String, Object> plano() {
        return Map.of(
                "nome", "Site de Casamento",
                "valorCriacao", mercadoPagoService.getValorCriacao(),
                "valorMensal", mercadoPagoService.getValorMensal(),
                "descricaoCriacao", "Criação do site + 1º mês incluso",
                "descricaoMensal", "A partir do 2º mês",
                "permanenciaMinimaMeses", 4,
                "arrependimentoDias", 7,
                "mpConfigurado", mercadoPagoService.configurado()
        );
    }

    /**
     * Landing: cadastro + gera checkout Mercado Pago (R$ 99).
     * Body: nomeNoiva, nomeNoivo, slug, email, senha
     */
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody Map<String, String> body) {
        try {
            Map<String, Object> resultado = assinaturaService.iniciarCheckout(
                    str(body.get("nomeNoiva")),
                    str(body.get("nomeNoivo")),
                    str(body.get("slug")),
                    str(body.get("email")),
                    str(body.get("senha"))
            );
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Não foi possível iniciar o checkout. Tente de novo.");
        }
    }

    /**
     * Após o pagamento dos R$ 99: ativa o site e devolve o link da mensalidade (R$ 49,90).
     * Body opcional: paymentId, externalReference (ex: site:123)
     */
    @PostMapping("/mensalidade")
    public ResponseEntity<?> mensalidade(@RequestBody Map<String, String> body) {
        try {
            Map<String, Object> resultado = assinaturaService.iniciarMensalidadeAposCriacao(
                    str(body.get("paymentId")),
                    str(body.get("externalReference"))
            );
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(e.getMessage());
        } catch (Exception e) {
            String detalhe = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.internalServerError()
                    .body("Não foi possível gerar o link da mensalidade: " + detalhe);
        }
    }

    private static String str(String v) {
        return v == null ? "" : v.trim();
    }
}
