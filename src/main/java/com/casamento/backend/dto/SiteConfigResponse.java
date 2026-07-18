package com.casamento.backend.dto;

import java.util.List;
import java.util.Map;

/**
 * Config pública do site (o que o front do casamento precisa renderizar).
 */
public class SiteConfigResponse {

    private String siteId;
    private String nomeNoiva;
    private String nomeNoivo;
    private String nomeCurto;
    private String dataCasamento;
    private String horaCasamento;
    private String diaSemana;
    private String mesExtenso;
    private String paisNoiva;
    private String paisNoivo;
    private String localNome;
    private String mapsUrl;
    private String fotoHeroUrl;
    private String fotoSecundariaUrl;
    private String fotoLocalUrl;
    private String fotoRodapeUrl;
    private List<String> fotosCarrossel;
    private Map<String, String> cores;
    private String pixChave;
    private String pixNomeRecebedor;
    private String pixCidade;
    private String musicaUrl;

    public String getSiteId() { return siteId; }
    public void setSiteId(String siteId) { this.siteId = siteId; }

    public String getNomeNoiva() { return nomeNoiva; }
    public void setNomeNoiva(String nomeNoiva) { this.nomeNoiva = nomeNoiva; }

    public String getNomeNoivo() { return nomeNoivo; }
    public void setNomeNoivo(String nomeNoivo) { this.nomeNoivo = nomeNoivo; }

    public String getNomeCurto() { return nomeCurto; }
    public void setNomeCurto(String nomeCurto) { this.nomeCurto = nomeCurto; }

    public String getDataCasamento() { return dataCasamento; }
    public void setDataCasamento(String dataCasamento) { this.dataCasamento = dataCasamento; }

    public String getHoraCasamento() { return horaCasamento; }
    public void setHoraCasamento(String horaCasamento) { this.horaCasamento = horaCasamento; }

    public String getDiaSemana() { return diaSemana; }
    public void setDiaSemana(String diaSemana) { this.diaSemana = diaSemana; }

    public String getMesExtenso() { return mesExtenso; }
    public void setMesExtenso(String mesExtenso) { this.mesExtenso = mesExtenso; }

    public String getPaisNoiva() { return paisNoiva; }
    public void setPaisNoiva(String paisNoiva) { this.paisNoiva = paisNoiva; }

    public String getPaisNoivo() { return paisNoivo; }
    public void setPaisNoivo(String paisNoivo) { this.paisNoivo = paisNoivo; }

    public String getLocalNome() { return localNome; }
    public void setLocalNome(String localNome) { this.localNome = localNome; }

    public String getMapsUrl() { return mapsUrl; }
    public void setMapsUrl(String mapsUrl) { this.mapsUrl = mapsUrl; }

    public String getFotoHeroUrl() { return fotoHeroUrl; }
    public void setFotoHeroUrl(String fotoHeroUrl) { this.fotoHeroUrl = fotoHeroUrl; }

    public String getFotoSecundariaUrl() { return fotoSecundariaUrl; }
    public void setFotoSecundariaUrl(String fotoSecundariaUrl) { this.fotoSecundariaUrl = fotoSecundariaUrl; }

    public String getFotoLocalUrl() { return fotoLocalUrl; }
    public void setFotoLocalUrl(String fotoLocalUrl) { this.fotoLocalUrl = fotoLocalUrl; }

    public String getFotoRodapeUrl() { return fotoRodapeUrl; }
    public void setFotoRodapeUrl(String fotoRodapeUrl) { this.fotoRodapeUrl = fotoRodapeUrl; }

    public List<String> getFotosCarrossel() { return fotosCarrossel; }
    public void setFotosCarrossel(List<String> fotosCarrossel) { this.fotosCarrossel = fotosCarrossel; }

    public Map<String, String> getCores() { return cores; }
    public void setCores(Map<String, String> cores) { this.cores = cores; }

    public String getPixChave() { return pixChave; }
    public void setPixChave(String pixChave) { this.pixChave = pixChave; }

    public String getPixNomeRecebedor() { return pixNomeRecebedor; }
    public void setPixNomeRecebedor(String pixNomeRecebedor) { this.pixNomeRecebedor = pixNomeRecebedor; }

    public String getPixCidade() { return pixCidade; }
    public void setPixCidade(String pixCidade) { this.pixCidade = pixCidade; }

    public String getMusicaUrl() { return musicaUrl; }
    public void setMusicaUrl(String musicaUrl) { this.musicaUrl = musicaUrl; }
}
