package com.casamento.backend.service;

import com.casamento.backend.dto.SiteConfigResponse;
import com.casamento.backend.model.Site;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SiteConfigService {

    private final ObjectMapper objectMapper;

    public SiteConfigService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SiteConfigResponse toResponse(Site site) {
        SiteConfigResponse r = new SiteConfigResponse();
        r.setSiteId(site.getSlug());
        r.setNomeNoiva(site.getNomeNoiva());
        r.setNomeNoivo(site.getNomeNoivo());
        r.setNomeCurto(site.getNomeCurto());
        r.setDataCasamento(site.getDataCasamento() != null ? site.getDataCasamento().toString() : null);
        r.setHoraCasamento(site.getHoraCasamento());
        r.setDiaSemana(site.getDiaSemana());
        r.setMesExtenso(site.getMesExtenso());
        r.setPaisNoiva(site.getPaisNoiva());
        r.setPaisNoivo(site.getPaisNoivo());
        r.setLocalNome(site.getLocalNome());
        r.setMapsUrl(site.getMapsUrl());
        r.setMesmoLocal(site.isMesmoLocal());
        r.setLocalNomeFesta(site.getLocalNomeFesta());
        r.setMapsUrlFesta(site.getMapsUrlFesta());
        r.setVersiculo(site.getVersiculo());
        r.setFraseBencao(site.getFraseBencao());
        r.setTituloGaleria(site.getTituloGaleria());
        r.setHistoriaCurta(site.getHistoriaCurta());
        r.setFotoHeroUrl(site.getFotoHeroUrl());
        r.setFotoSecundariaUrl(site.getFotoSecundariaUrl());
        r.setFotoLocalUrl(site.getFotoLocalUrl());
        r.setFotoRodapeUrl(site.getFotoRodapeUrl());
        r.setFotosCarrossel(parseListaUrls(site.getFotosCarrossel()));
        r.setCores(montarCores(site));
        r.setPixChave(site.getPixChave());
        r.setPixNomeRecebedor(site.getPixNomeRecebedor());
        r.setPixCidade(site.getPixCidade());
        r.setMusicaUrl(site.getMusicaUrl());
        r.setFonteNomes(site.getFonteNomes());
        r.setMpCartaoDisponivel(site.isMpSellerConectado());
        return r;
    }

    public List<String> parseListaUrls(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public String toJsonListaUrls(List<String> urls) {
        try {
            return objectMapper.writeValueAsString(urls != null ? urls : List.of());
        } catch (Exception e) {
            return "[]";
        }
    }

    private Map<String, String> montarCores(Site site) {
        Map<String, String> cores = new LinkedHashMap<>();
        putSePresente(cores, "verde", site.getCorVerde());
        putSePresente(cores, "verdeEscuro", site.getCorVerdeEscuro());
        putSePresente(cores, "verdeClaro", site.getCorVerdeClaro());
        putSePresente(cores, "fundo", site.getCorFundo());
        putSePresente(cores, "fundoBege", site.getCorFundoBege());
        putSePresente(cores, "texto", site.getCorTexto());
        putSePresente(cores, "textoHero", site.getCorTextoHero());
        return cores;
    }

    private void putSePresente(Map<String, String> map, String chave, String valor) {
        if (valor != null && !valor.isBlank()) {
            map.put(chave, valor.trim());
        }
    }
}
