package com.casamento.backend.controller;

import com.casamento.backend.model.Site;
import com.casamento.backend.repository.SiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controller de Sites = cadastro de CASAMENTOS (clientes).
 *
 * Ideia do negócio:
 * - 1 Site = 1 casal / 1 cliente
 * - Depois o front desse casal manda X-Site-Id: slug
 * - Presentes, presença e PIX ficam isolados por site
 *
 * Rotas (todas pedem login admin JWT, porque estão em /api/admin/...):
 * - GET  /api/admin/sites          → lista todos os casamentos
 * - POST /api/admin/sites          → cria um casamento novo
 * - PATCH /api/admin/sites/{id}/desativar → desliga um site
 *
 * IMPORTANTE: aqui NÃO usamos SiteContext / X-Site-Id.
 * Porque você está CRIANDO o site — ele ainda não existe no header.
 */
@RestController                 // Diz: esta classe responde HTTP (API REST)
@RequestMapping("/api/admin/sites") // Prefixo de todas as rotas desta classe
@CrossOrigin(origins = "*")     // Permite o front (ou Postman) chamar de outro domínio
public class AdminSiteController {

    // O Spring injeta sozinho a "porta" pro banco da tabela site
    @Autowired
    private SiteRepository siteRepository;

    /**
     * GET /api/admin/sites
     * Devolve a lista de todos os sites já cadastrados.
     * Útil pra você ver: "rafaekevin", "mariaejoao", etc.
     */
    @GetMapping
    public List<Site> listar() {
        // findAll() já vem pronto do JpaRepository
        return siteRepository.findAll();
    }

    /**
     * POST /api/admin/sites
     * Body JSON esperado (exemplo):
     * {
     *   "slug": "mariaejoao",
     *   "nomeNoiva": "Maria",
     *   "nomeNoivo": "João",
     *   "dataCasamento": "2027-12-10"
     * }
     *
     * Usamos Map&lt;String, Object&gt; pra ler o JSON campo a campo
     * (forma simples de aprender; depois pode virar um DTO).
     */
    @PostMapping
    public ResponseEntity<?> criar(@RequestBody Map<String, Object> body) {

        // 1) Lê e limpa os campos que vieram no JSON
        String slug = normalizarSlug(asString(body.get("slug")));
        String nomeNoiva = asString(body.get("nomeNoiva"));
        String nomeNoivo = asString(body.get("nomeNoivo"));
        LocalDate dataCasamento = parseData(body.get("dataCasamento"));

        String pixChave = asString(body.get("pixChave"));
        String pixNomeRecebedor = asString(body.get("pixNomeRecebedor"));
        String pixCidade = asString(body.get("pixCidade"));

        // 2) Validações básicas (se faltar algo importante, devolve 400)
        if (slug.isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Informe o slug (ex: mariaejoao).");
        }
        if (nomeNoiva.isBlank() || nomeNoivo.isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Informe nomeNoiva e nomeNoivo.");
        }

        // 3) Slug precisa ser único (não pode ter dois "mariaejoao")
        if (siteRepository.findBySlug(slug).isPresent()) {
            // 409 Conflict = "já existe / conflito"
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Já existe um site com o slug \"" + slug + "\".");
        }

        // 4) Monta o objeto Site (ainda não está no banco)
        Site site = new Site();
        site.setSlug(slug);
        site.setNomeNoiva(nomeNoiva);
        site.setNomeNoivo(nomeNoivo);
        site.setDataCasamento(dataCasamento);
        site.setAtivo(true); // começa ligado

        site.setPixChave(pixChave.isBlank() ? null : pixChave);
        site.setPixNomeRecebedor(pixNomeRecebedor.isBlank() ? null : pixNomeRecebedor);
        site.setPixCidade(pixCidade.isBlank() ? null : pixCidade);

        // 5) save() grava no Postgres e devolve o Site já com id gerado
        Site salvo = siteRepository.save(site);

        // 201 Created = "nasceu com sucesso"
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    /**
     * PATCH /api/admin/sites/{id}/desativar
     * Não apaga os dados — só marca ativo = false.
     * Assim o SiteFilter para de aceitar aquele slug.
     */
    @PatchMapping("/{id}/desativar")
    public ResponseEntity<?> desativar(@PathVariable Long id) {
        return siteRepository.findById(id)
                .map(site -> {
                    site.setAtivo(false);
                    return ResponseEntity.ok(siteRepository.save(site));
                })
                // Se não achar o id → 404
                .orElse(ResponseEntity.notFound().build());
    }

    // ----------------- HELPERS (funções auxiliares) -----------------

    /**
     * Transforma o slug em algo seguro pro header:
     * - minúsculo
     * - sem espaço
     * - só letras, números e hífen
     *
     * Ex.: "Maria e João!" → "mariaejoo" (sem o ! e espaços)
     * Melhor já mandar prontinho: "mariaejoao"
     */
    private static String normalizarSlug(String slug) {
        if (slug == null) {
            return "";
        }
        return slug.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9-]", "");
    }

    /**
     * Converte qualquer valor do JSON em String limpa.
     * Se vier null → "" (string vazia).
     */
    private static String asString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * Converte "2027-12-10" em LocalDate.
     * Se não vier data, retorna null (é opcional).
     */
    private static LocalDate parseData(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return LocalDate.parse(String.valueOf(value).trim());
    }
}