package com.casamento.backend.controller;

import com.casamento.backend.config.SiteContext;
import com.casamento.backend.config.SiteLimites;
import com.casamento.backend.model.Site;
import com.casamento.backend.repository.SiteRepository;
import com.casamento.backend.service.FileStorageService;
import com.casamento.backend.service.SiteConfigService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Personalização do site ATUAL (via X-Site-Id).
 * A noiva edita textos, cores, PIX e faz upload das fotos do layout.
 */
@RestController
@RequestMapping("/api/admin/site")
@CrossOrigin(origins = "*")
public class AdminSitePersonalizacaoController {

    private static final Set<String> FONTES_NOMES_VALIDAS = Set.of(
            "great-vibes",
            "allura",
            "pinyon-script",
            "alex-brush",
            "tangerine",
            "sacramento",
            "parisienne",
            "meie-script",
            "monsieur-la-doulaise",
            "bona-nova",
            "playfair-italic"
    );

    private final SiteRepository siteRepository;
    private final SiteConfigService siteConfigService;
    private final FileStorageService fileStorageService;

    public AdminSitePersonalizacaoController(
            SiteRepository siteRepository,
            SiteConfigService siteConfigService,
            FileStorageService fileStorageService) {
        this.siteRepository = siteRepository;
        this.siteConfigService = siteConfigService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/atual")
    public ResponseEntity<?> obterAtual() {
        Site site = SiteContext.get();
        if (site == null) {
            return ResponseEntity.badRequest()
                    .body("Informe o header X-Site-Id com o slug do casamento.");
        }
        return ResponseEntity.ok(siteConfigService.toResponse(site));
    }

    @PutMapping("/atual")
    public ResponseEntity<?> atualizar(@RequestBody Map<String, Object> body) {
        Site site = SiteContext.get();
        if (site == null) {
            return ResponseEntity.badRequest()
                    .body("Informe o header X-Site-Id com o slug do casamento.");
        }

        if (body.containsKey("nomeNoiva")) {
            String v = asString(body.get("nomeNoiva"));
            if (v.isBlank()) {
                return ResponseEntity.badRequest().body("nomeNoiva não pode ficar vazio.");
            }
            site.setNomeNoiva(v);
        }
        if (body.containsKey("nomeNoivo")) {
            String v = asString(body.get("nomeNoivo"));
            if (v.isBlank()) {
                return ResponseEntity.badRequest().body("nomeNoivo não pode ficar vazio.");
            }
            site.setNomeNoivo(v);
        }
        if (body.containsKey("nomeCurto")) {
            site.setNomeCurto(blankToNull(asString(body.get("nomeCurto"))));
        }
        if (body.containsKey("dataCasamento")) {
            site.setDataCasamento(parseData(body.get("dataCasamento")));
        }
        if (body.containsKey("horaCasamento")) {
            site.setHoraCasamento(blankToNull(asString(body.get("horaCasamento"))));
        }
        if (body.containsKey("diaSemana")) {
            site.setDiaSemana(blankToNull(asString(body.get("diaSemana"))));
        }
        if (body.containsKey("mesExtenso")) {
            site.setMesExtenso(blankToNull(asString(body.get("mesExtenso"))));
        }
        if (body.containsKey("paisNoiva")) {
            site.setPaisNoiva(blankToNull(asString(body.get("paisNoiva"))));
        }
        if (body.containsKey("paisNoivo")) {
            site.setPaisNoivo(blankToNull(asString(body.get("paisNoivo"))));
        }
        if (body.containsKey("localNome")) {
            site.setLocalNome(blankToNull(asString(body.get("localNome"))));
        }
        if (body.containsKey("mapsUrl")) {
            site.setMapsUrl(blankToNull(asString(body.get("mapsUrl"))));
        }
        if (body.containsKey("versiculo")) {
            site.setVersiculo(blankToNull(asString(body.get("versiculo"))));
        }
        if (body.containsKey("fraseBencao")) {
            site.setFraseBencao(blankToNull(asString(body.get("fraseBencao"))));
        }
        if (body.containsKey("tituloGaleria")) {
            String titulo = asString(body.get("tituloGaleria"));
            if (titulo.length() > 80) {
                return ResponseEntity.badRequest().body("Título da galeria: no máximo 80 caracteres.");
            }
            site.setTituloGaleria(blankToNull(titulo));
        }
        if (body.containsKey("historiaCurta")) {
            String hist = asString(body.get("historiaCurta"));
            if (hist.length() > 500) {
                return ResponseEntity.badRequest().body("História curta: no máximo 500 caracteres.");
            }
            site.setHistoriaCurta(blankToNull(hist));
        }
        if (body.containsKey("pixChave")) {
            site.setPixChave(blankToNull(asString(body.get("pixChave"))));
        }
        if (body.containsKey("pixNomeRecebedor")) {
            site.setPixNomeRecebedor(blankToNull(asString(body.get("pixNomeRecebedor"))));
        }
        if (body.containsKey("pixCidade")) {
            site.setPixCidade(blankToNull(asString(body.get("pixCidade"))));
        }
        if (body.containsKey("musicaUrl")) {
            site.setMusicaUrl(blankToNull(asString(body.get("musicaUrl"))));
        }
        if (body.containsKey("fonteNomes")) {
            String fonte = asString(body.get("fonteNomes"));
            if (fonte.isBlank()) {
                site.setFonteNomes(null);
            } else if (!FONTES_NOMES_VALIDAS.contains(fonte)) {
                return ResponseEntity.badRequest().body("fonteNomes inválida.");
            } else {
                site.setFonteNomes(fonte);
            }
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> cores = body.get("cores") instanceof Map
                ? (Map<String, Object>) body.get("cores")
                : null;
        if (cores != null) {
            if (cores.containsKey("verde")) site.setCorVerde(blankToNull(asString(cores.get("verde"))));
            if (cores.containsKey("verdeEscuro")) site.setCorVerdeEscuro(blankToNull(asString(cores.get("verdeEscuro"))));
            if (cores.containsKey("verdeClaro")) site.setCorVerdeClaro(blankToNull(asString(cores.get("verdeClaro"))));
            if (cores.containsKey("fundo")) site.setCorFundo(blankToNull(asString(cores.get("fundo"))));
            if (cores.containsKey("fundoBege")) site.setCorFundoBege(blankToNull(asString(cores.get("fundoBege"))));
            if (cores.containsKey("texto")) site.setCorTexto(blankToNull(asString(cores.get("texto"))));
            if (cores.containsKey("textoHero")) site.setCorTextoHero(blankToNull(asString(cores.get("textoHero"))));
        }

        Site salvo = siteRepository.save(site);
        return ResponseEntity.ok(siteConfigService.toResponse(salvo));
    }

    /**
     * Upload de mídia do layout.
     * tipo = hero | secundaria | local | carrossel
     * carrosselIndex (opcional, 0-based) — se omitido no carrossel, adiciona no fim.
     */
    @PostMapping(value = "/atual/midia", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMidia(
            @RequestParam("tipo") String tipo,
            @RequestParam(value = "carrosselIndex", required = false) Integer carrosselIndex,
            @RequestParam("arquivo") MultipartFile arquivo) {

        Site site = SiteContext.get();
        if (site == null) {
            return ResponseEntity.badRequest()
                    .body("Informe o header X-Site-Id com o slug do casamento.");
        }

        String tipoNorm = tipo == null ? "" : tipo.trim().toLowerCase();
        if (!List.of("hero", "secundaria", "local", "rodape", "carrossel", "musica").contains(tipoNorm)) {
            return ResponseEntity.badRequest()
                    .body("tipo inválido. Use: hero, secundaria, local, rodape, carrossel ou musica.");
        }

        try {
            String pasta = "site-" + site.getSlug();

            if ("musica".equals(tipoNorm)) {
                String url = fileStorageService.salvarAudio(arquivo, pasta + "/musicas");
                trocarAudio(site.getMusicaUrl(), url);
                site.setMusicaUrl(url);
                Site salvo = siteRepository.save(site);
                return ResponseEntity.ok(siteConfigService.toResponse(salvo));
            }

            String url = fileStorageService.salvarImagem(arquivo, pasta);

            switch (tipoNorm) {
                case "hero" -> {
                    trocarUrl(site.getFotoHeroUrl(), url);
                    site.setFotoHeroUrl(url);
                }
                case "secundaria" -> {
                    trocarUrl(site.getFotoSecundariaUrl(), url);
                    site.setFotoSecundariaUrl(url);
                }
                case "local" -> {
                    trocarUrl(site.getFotoLocalUrl(), url);
                    site.setFotoLocalUrl(url);
                }
                case "rodape" -> {
                    trocarUrl(site.getFotoRodapeUrl(), url);
                    site.setFotoRodapeUrl(url);
                }
                case "carrossel" -> {
                    List<String> fotos = new ArrayList<>(siteConfigService.parseListaUrls(site.getFotosCarrossel()));
                    if (carrosselIndex != null && carrosselIndex >= 0 && carrosselIndex < fotos.size()) {
                        trocarUrl(fotos.get(carrosselIndex), url);
                        fotos.set(carrosselIndex, url);
                    } else if (fotos.size() >= SiteLimites.MAX_FOTOS_CARROSSEL) {
                        return ResponseEntity.badRequest()
                                .body("Máximo de " + SiteLimites.MAX_FOTOS_CARROSSEL + " fotos no carrossel.");
                    } else {
                        fotos.add(url);
                    }
                    site.setFotosCarrossel(siteConfigService.toJsonListaUrls(fotos));
                }
            }

            Site salvo = siteRepository.save(site);
            return ResponseEntity.ok(siteConfigService.toResponse(salvo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Falha ao enviar o arquivo.";
            return ResponseEntity.internalServerError().body(msg);
        }
    }

    @DeleteMapping("/atual/midia/carrossel/{index}")
    public ResponseEntity<?> removerFotoCarrossel(@PathVariable int index) {
        Site site = SiteContext.get();
        if (site == null) {
            return ResponseEntity.badRequest()
                    .body("Informe o header X-Site-Id com o slug do casamento.");
        }

        List<String> fotos = new ArrayList<>(siteConfigService.parseListaUrls(site.getFotosCarrossel()));
        if (index < 0 || index >= fotos.size()) {
            return ResponseEntity.badRequest().body("Índice inválido no carrossel.");
        }

        String removida = fotos.remove(index);
        fileStorageService.excluirImagem(removida);
        site.setFotosCarrossel(siteConfigService.toJsonListaUrls(fotos));
        Site salvo = siteRepository.save(site);
        return ResponseEntity.ok(siteConfigService.toResponse(salvo));
    }

    private void trocarUrl(String antiga, String nova) {
        if (antiga != null && !antiga.isBlank() && !antiga.equals(nova)) {
            fileStorageService.excluirImagem(antiga);
        }
    }

    private void trocarAudio(String antiga, String nova) {
        if (antiga != null && !antiga.isBlank() && !antiga.equals(nova) && antiga.startsWith("http")) {
            fileStorageService.excluirAudio(antiga);
        }
    }

    private static String asString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static LocalDate parseData(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return LocalDate.parse(String.valueOf(value).trim());
    }
}
