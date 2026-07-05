package com.casamento.backend.dto;

import java.util.List;

public class CompraCarrinhoRequest {

    private String nomeComprador;
    private List<ItemCarrinho> itens;

    public String getNomeComprador() {
        return nomeComprador;
    }

    public void setNomeComprador(String nomeComprador) {
        this.nomeComprador = nomeComprador;
    }

    public List<ItemCarrinho> getItens() {
        return itens;
    }

    public void setItens(List<ItemCarrinho> itens) {
        this.itens = itens;
    }

    public static class ItemCarrinho {
        private Long presenteId;
        private Integer quantidade;

        public Long getPresenteId() {
            return presenteId;
        }

        public void setPresenteId(Long presenteId) {
            this.presenteId = presenteId;
        }

        public Integer getQuantidade() {
            return quantidade;
        }

        public void setQuantidade(Integer quantidade) {
            this.quantidade = quantidade;
        }
    }
}
