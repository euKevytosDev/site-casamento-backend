package com.casamento.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    /** Versículo bíblico exibido no convite */
    @Column(name = "versiculo", columnDefinition = "TEXT")
    private String versiculo;

    /** Frase acima dos nomes dos pais (ex.: Com a bênção de Deus...) */
    @Column(name = "frase_bencao", length = 300)
    private String fraseBencao;

    /** Título acima do carrossel (ex.: Nossos momentos) */
    @Column(name = "titulo_galeria", length = 80)
    private String tituloGaleria;

    /** História curta do casal (2–4 linhas; convidado não lê texto longo) */
    @Column(name = "historia_curta", length = 500)
    private String historiaCurta;

    @Column(name = "foto_hero_url", length = 500)
    private String fotoHeroUrl;

    @Column(name = "foto_secundaria_url", length = 500)
    private String fotoSecundariaUrl;

    @Column(name = "foto_local_url", length = 500)
    private String fotoLocalUrl;

    /** Foto final do rodapé (bloco .fotodois) */
    @Column(name = "foto_rodape_url", length = 500)
    private String fotoRodapeUrl;

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

    /** Access Token OAuth da conta Mercado Pago da noiva (presentes no cartão). */
    @Column(name = "mp_seller_access_token", length = 500)
    private String mpSellerAccessToken;

    @Column(name = "mp_seller_refresh_token", length = 500)
    private String mpSellerRefreshToken;

    @Column(name = "mp_seller_user_id", length = 80)
    private String mpSellerUserId;

    @Column(name = "mp_seller_conectado_em")
    private java.time.Instant mpSellerConectadoEm;

    /** Caminho/URL da música de fundo (ex.: musicas/Blessings.mp3) */
    @Column(name = "musica_url", length = 500)
    private String musicaUrl;

    /** PENDENTE | ATIVA | ATRASADA | CANCELADA */
    @Column(name = "assinatura_status", length = 30)
    private String assinaturaStatus = "ATIVA";

    @Column(name = "mp_preference_id", length = 120)
    private String mpPreferenceId;

    @Column(name = "mp_payment_id", length = 120)
    private String mpPaymentId;

    @Column(name = "mp_preapproval_id", length = 120)
    private String mpPreapprovalId;

    /** Link do Checkout Pro da assinatura mensal (autorizar cartão). */
    @Column(name = "mp_assinatura_init_point", length = 500)
    private String mpAssinaturaInitPoint;

    /** pending | authorized | paused | cancelled */
    @Column(name = "mp_assinatura_status", length = 40)
    private String mpAssinaturaStatus;

    @Column(name = "assinatura_inicio")
    private java.time.Instant assinaturaInicio;

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

    public String getVersiculo() { return versiculo; }
    public void setVersiculo(String versiculo) { this.versiculo = versiculo; }

    public String getFraseBencao() { return fraseBencao; }
    public void setFraseBencao(String fraseBencao) { this.fraseBencao = fraseBencao; }

    public String getTituloGaleria() { return tituloGaleria; }
    public void setTituloGaleria(String tituloGaleria) { this.tituloGaleria = tituloGaleria; }

    public String getHistoriaCurta() { return historiaCurta; }
    public void setHistoriaCurta(String historiaCurta) { this.historiaCurta = historiaCurta; }

    public String getFotoHeroUrl() { return fotoHeroUrl; }
    public void setFotoHeroUrl(String fotoHeroUrl) { this.fotoHeroUrl = fotoHeroUrl; }

    public String getFotoSecundariaUrl() { return fotoSecundariaUrl; }
    public void setFotoSecundariaUrl(String fotoSecundariaUrl) { this.fotoSecundariaUrl = fotoSecundariaUrl; }

    public String getFotoLocalUrl() { return fotoLocalUrl; }
    public void setFotoLocalUrl(String fotoLocalUrl) { this.fotoLocalUrl = fotoLocalUrl; }

    public String getFotoRodapeUrl() { return fotoRodapeUrl; }
    public void setFotoRodapeUrl(String fotoRodapeUrl) { this.fotoRodapeUrl = fotoRodapeUrl; }

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

    @JsonIgnore
    public String getMpSellerAccessToken() { return mpSellerAccessToken; }
    public void setMpSellerAccessToken(String mpSellerAccessToken) { this.mpSellerAccessToken = mpSellerAccessToken; }

    @JsonIgnore
    public String getMpSellerRefreshToken() { return mpSellerRefreshToken; }
    public void setMpSellerRefreshToken(String mpSellerRefreshToken) { this.mpSellerRefreshToken = mpSellerRefreshToken; }

    public String getMpSellerUserId() { return mpSellerUserId; }
    public void setMpSellerUserId(String mpSellerUserId) { this.mpSellerUserId = mpSellerUserId; }

    public java.time.Instant getMpSellerConectadoEm() { return mpSellerConectadoEm; }
    public void setMpSellerConectadoEm(java.time.Instant mpSellerConectadoEm) { this.mpSellerConectadoEm = mpSellerConectadoEm; }

    public boolean isMpSellerConectado() {
        return mpSellerAccessToken != null && !mpSellerAccessToken.isBlank();
    }

    public String getMusicaUrl() { return musicaUrl; }
    public void setMusicaUrl(String musicaUrl) { this.musicaUrl = musicaUrl; }

    public String getAssinaturaStatus() { return assinaturaStatus; }
    public void setAssinaturaStatus(String assinaturaStatus) { this.assinaturaStatus = assinaturaStatus; }

    public String getMpPreferenceId() { return mpPreferenceId; }
    public void setMpPreferenceId(String mpPreferenceId) { this.mpPreferenceId = mpPreferenceId; }

    public String getMpPaymentId() { return mpPaymentId; }
    public void setMpPaymentId(String mpPaymentId) { this.mpPaymentId = mpPaymentId; }

    public String getMpPreapprovalId() { return mpPreapprovalId; }
    public void setMpPreapprovalId(String mpPreapprovalId) { this.mpPreapprovalId = mpPreapprovalId; }

    public String getMpAssinaturaInitPoint() { return mpAssinaturaInitPoint; }
    public void setMpAssinaturaInitPoint(String mpAssinaturaInitPoint) { this.mpAssinaturaInitPoint = mpAssinaturaInitPoint; }

    public String getMpAssinaturaStatus() { return mpAssinaturaStatus; }
    public void setMpAssinaturaStatus(String mpAssinaturaStatus) { this.mpAssinaturaStatus = mpAssinaturaStatus; }

    public java.time.Instant getAssinaturaInicio() { return assinaturaInicio; }
    public void setAssinaturaInicio(java.time.Instant assinaturaInicio) { this.assinaturaInicio = assinaturaInicio; }
}
