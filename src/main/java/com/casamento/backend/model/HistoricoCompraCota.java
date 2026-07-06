package com.casamento.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "historico_compra_cota")
public class HistoricoCompraCota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "presente_id")
    private Long presenteId;

    @Column(name = "nome_presente", nullable = false)
    private String nomePresente;

    @Column(name = "nome_comprador", nullable = false)
    private String nomeComprador;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "valor_cota", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorCota;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "data_compra", nullable = false)
    private LocalDateTime dataCompra = LocalDateTime.now();

    public HistoricoCompraCota() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPresenteId() {
        return presenteId;
    }

    public void setPresenteId(Long presenteId) {
        this.presenteId = presenteId;
    }

    public String getNomePresente() {
        return nomePresente;
    }

    public void setNomePresente(String nomePresente) {
        this.nomePresente = nomePresente;
    }

    public String getNomeComprador() {
        return nomeComprador;
    }

    public void setNomeComprador(String nomeComprador) {
        this.nomeComprador = nomeComprador;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getValorCota() {
        return valorCota;
    }

    public void setValorCota(BigDecimal valorCota) {
        this.valorCota = valorCota;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public LocalDateTime getDataCompra() {
        return dataCompra;
    }

    public void setDataCompra(LocalDateTime dataCompra) {
        this.dataCompra = dataCompra;
    }
}
