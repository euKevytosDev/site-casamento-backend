package com.casamento.backend.dto;

import java.math.BigDecimal;

public class GerarPixResponse {

    private BigDecimal total;
    private String pixCopiaCola;
    private String txid;
    private String mensagem;

    public GerarPixResponse(BigDecimal total, String pixCopiaCola, String txid, String mensagem) {
        this.total = total;
        this.pixCopiaCola = pixCopiaCola;
        this.txid = txid;
        this.mensagem = mensagem;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getPixCopiaCola() {
        return pixCopiaCola;
    }

    public void setPixCopiaCola(String pixCopiaCola) {
        this.pixCopiaCola = pixCopiaCola;
    }

    public String getTxid() {
        return txid;
    }

    public void setTxid(String txid) {
        this.txid = txid;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
