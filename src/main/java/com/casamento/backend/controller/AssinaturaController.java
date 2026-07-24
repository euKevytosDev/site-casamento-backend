package com.casamento.backend.controller;

import com.casamento.backend.service.AsaasService;
import com.casamento.backend.service.AssinaturaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/assinatura")
@CrossOrigin(origins = "*")
public class AssinaturaController {

    private final AssinaturaService assinaturaService;
    private final AsaasService asaasService;

    public AssinaturaController(AssinaturaService assinaturaService, AsaasService asaasService) {
        this.assinaturaService = assinaturaService;
        this.asaasService = asaasService;
    }

    @GetMapping("/plano")
    public Map<String, Object> plano() {
        int permanencia = asaasService.getPermanenciaMinimaMeses();
        int trialDias = asaasService.getTrialDias();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("nome", "Site de Casamento");
        out.put("valorMensal", asaasService.getValorMensal());
        out.put("descricaoMensal", trialDias > 0
                ? "Plano único — " + trialDias + " dias grátis, depois mensal"
                : "Plano único — assinatura mensal, cancele quando quiser");
        out.put("permanenciaMinimaMeses", permanencia);
        out.put("cancelamentoLivre", permanencia <= 0);
        out.put("trialDias", trialDias);
        out.put("arrependimentoDias", 7);
        out.put("gateway", "asaas");
        out.put("asaasConfigurado", asaasService.configurado());
        out.put("mpConfigurado", asaasService.configurado()); // compat landing antiga
        out.put("modoTeste", asaasService.modoSandbox());
        return out;
    }

    /**
     * Landing: cadastro + checkout de assinatura mensal (Asaas).
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
                    str(body.get("cpf")),
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
