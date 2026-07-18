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
        int permanencia = mercadoPagoService.getPermanenciaMinimaMeses();
        return Map.of(
                "nome", "Site de Casamento",
                "valorMensal", mercadoPagoService.getValorMensal(),
                "descricaoMensal", "Plano único — assinatura mensal, cancele quando quiser",
                "permanenciaMinimaMeses", permanencia,
                "cancelamentoLivre", permanencia <= 0,
                "arrependimentoDias", 7,
                "mpConfigurado", mercadoPagoService.configurado(),
                "modoTeste", mercadoPagoService.modoTeste()
        );
    }

    /**
     * Landing: cadastro + um único checkout de assinatura mensal.
     */
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody Map<String, String> body) {
        try {
            Map<String, Object> resultado = assinaturaService.iniciarCheckout(
                    str(body.get("nomeNoiva")),
                    str(body.get("nomeNoivo")),
                    str(body.get("slug")),
                    str(body.get("email")),
                    str(body.get("senha")),
                    str(body.get("emailPagador"))
            );
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(e.getMessage());
        } catch (Exception e) {
            String detalhe = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.internalServerError()
                    .body("Não foi possível iniciar o checkout: " + detalhe);
        }
    }

    private static String str(String v) {
        return v == null ? "" : v.trim();
    }
}
