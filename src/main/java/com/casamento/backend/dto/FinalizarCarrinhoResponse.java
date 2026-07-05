package com.casamento.backend.dto;

import java.math.BigDecimal;

public class FinalizarCarrinhoResponse {

    private BigDecimal total;
    private String mensagem;

    public FinalizarCarrinhoResponse(BigDecimal total, String mensagem) {
        this.total = total;
        this.mensagem = mensagem;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
