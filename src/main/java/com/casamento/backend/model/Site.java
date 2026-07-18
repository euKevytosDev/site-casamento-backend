package com.casamento.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Entidade Site = 1 casamento / 1 cliente no banco.
 * Campos de conteúdo/visual permitem a noiva personalizar o site pelo admin.
 */
@Entity
@Table(name = "site")
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(name = "nome_noiva", nullable = false)
    private String nomeNoiva;

    @Column(name = "nome_noivo", nullable = false)
    private String nomeNoivo;

    @Column(name = "nome_curto", length = 80)
    private String nomeCurto;

    @Column(name = "data_casamento")
    private LocalDate dataCasamento;

    @Column(name = "hora_casamento", length = 10)
    private String horaCasamento;

    @Column(name = "dia_semana", length = 30)
    private String diaSemana;

    @Column(name = "mes_extenso", length = 30)
    private String mesExtenso;

    @Column(name = "pais_noiva")
    private String paisNoiva;

    @Column(name = "pais_noivo")
    private String paisNoivo;

    @Column(name = "local_nome")
    private String localNome;

    @Column(name = "maps_url", length = 500)
    private String mapsUrl;

    @Column(name = "foto_hero_url", length = 500)
    private String fotoHeroUrl;

    @Column(name = "foto_secundaria_url", length = 500)
    private String fotoSecundariaUrl;

    @Column(name = "foto_local_url", length = 500)
    private String fotoLocalUrl;

    /** JSON array de URLs: ["https://...","https://..."] */
    @Column(name = "fotos_carrossel", columnDefinition = "TEXT")
    private String fotosCarrossel;

    @Column(name = "cor_verde", length = 20)
    private String corVerde;

    @Column(name = "cor_verde_escuro", length = 20)
    private String corVerdeEscuro;

    @Column(name = "cor_verde_claro", length = 20)
    private String corVerdeClaro;

    @Column(name = "cor_fundo", length = 20)
    private String corFundo;

    @Column(name = "cor_fundo_bege", length = 20)
    private String corFundoBege;

    @Column(name = "cor_texto", length = 20)
    private String corTexto;

    @Column(name = "cor_texto_hero", length = 20)
    private String corTextoHero;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "pix_chave")
    private String pixChave;

    @Column(name = "pix_nome_recebedor")
    private String pixNomeRecebedor;

    @Column(name = "pix_cidade")
    private String pixCidade;

    public Site() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getNomeNoiva() { return nomeNoiva; }
    public void setNomeNoiva(String nomeNoiva) { this.nomeNoiva = nomeNoiva; }

    public String getNomeNoivo() { return nomeNoivo; }
    public void setNomeNoivo(String nomeNoivo) { this.nomeNoivo = nomeNoivo; }

    public String getNomeCurto() { return nomeCurto; }
    public void setNomeCurto(String nomeCurto) { this.nomeCurto = nomeCurto; }

    public LocalDate getDataCasamento() { return dataCasamento; }
    public void setDataCasamento(LocalDate dataCasamento) { this.dataCasamento = dataCasamento; }

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

    public String getFotosCarrossel() { return fotosCarrossel; }
    public void setFotosCarrossel(String fotosCarrossel) { this.fotosCarrossel = fotosCarrossel; }

    public String getCorVerde() { return corVerde; }
    public void setCorVerde(String corVerde) { this.corVerde = corVerde; }

    public String getCorVerdeEscuro() { return corVerdeEscuro; }
    public void setCorVerdeEscuro(String corVerdeEscuro) { this.corVerdeEscuro = corVerdeEscuro; }

    public String getCorVerdeClaro() { return corVerdeClaro; }
    public void setCorVerdeClaro(String corVerdeClaro) { this.corVerdeClaro = corVerdeClaro; }

    public String getCorFundo() { return corFundo; }
    public void setCorFundo(String corFundo) { this.corFundo = corFundo; }

    public String getCorFundoBege() { return corFundoBege; }
    public void setCorFundoBege(String corFundoBege) { this.corFundoBege = corFundoBege; }

    public String getCorTexto() { return corTexto; }
    public void setCorTexto(String corTexto) { this.corTexto = corTexto; }

    public String getCorTextoHero() { return corTextoHero; }
    public void setCorTextoHero(String corTextoHero) { this.corTextoHero = corTextoHero; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public String getPixChave() { return pixChave; }
    public void setPixChave(String pixChave) { this.pixChave = pixChave; }

    public String getPixNomeRecebedor() { return pixNomeRecebedor; }
    public void setPixNomeRecebedor(String pixNomeRecebedor) { this.pixNomeRecebedor = pixNomeRecebedor; }

    public String getPixCidade() { return pixCidade; }
    public void setPixCidade(String pixCidade) { this.pixCidade = pixCidade; }
}
