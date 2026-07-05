package com.casamento.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "presente_casamento")
public class PresenteCasamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private String imagem;

    @Column(nullable = false)
    private Boolean comprado = false;

    @Column(name = "cotas_total", nullable = false)
    private Integer cotasTotal = 10;

    @Column(name = "cotas_vendidas", nullable = false)
    private Integer cotasVendidas = 0;

    @Column(name = "nome_comprador")
    private String nomeComprador;

    @Column(name = "data_cadastro", insertable = true, updatable = false)
    private LocalDateTime dataCadastro = LocalDateTime.now();

    public PresenteCasamento() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getImagem() {
        return imagem;
    }

    public void setImagem(String imagem) {
        this.imagem = imagem;
    }

    public Boolean getComprado() {
        return comprado;
    }

    public void setComprado(Boolean comprado) {
        this.comprado = comprado;
    }

    public String getNomeComprador() {
        return nomeComprador;
    }

    public void setNomeComprador(String nomeComprador) {
        this.nomeComprador = nomeComprador;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDateTime dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public Integer getCotasTotal() {
        return cotasTotal;
    }

    public void setCotasTotal(Integer cotasTotal) {
        this.cotasTotal = cotasTotal != null && cotasTotal > 0 ? cotasTotal : 1;
    }

    public Integer getCotasVendidas() {
        return cotasVendidas;
    }

    public void setCotasVendidas(Integer cotasVendidas) {
        this.cotasVendidas = cotasVendidas != null && cotasVendidas >= 0 ? cotasVendidas : 0;
    }

    public int getCotasDisponiveis() {
        if (Boolean.TRUE.equals(comprado) && (cotasVendidas == null || cotasVendidas == 0)) {
            return 0;
        }
        int total = cotasTotal != null ? cotasTotal : 1;
        int vendidas = cotasVendidas != null ? cotasVendidas : 0;
        return Math.max(0, total - vendidas);
    }

    public void atualizarStatusComprado() {
        this.comprado = getCotasDisponiveis() <= 0;
    }
}
