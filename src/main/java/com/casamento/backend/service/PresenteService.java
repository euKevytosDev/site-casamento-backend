package com.casamento.backend.service;

import com.casamento.backend.dto.CompraCarrinhoRequest;
import com.casamento.backend.dto.FinalizarCarrinhoResponse;
import com.casamento.backend.model.PresenteCasamento;
import com.casamento.backend.repository.PresenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class PresenteService {

    @Autowired
    private PresenteRepository presenteRepository;

    @Transactional
    public FinalizarCarrinhoResponse finalizarCarrinho(CompraCarrinhoRequest request) {
        if (request.getNomeComprador() == null || request.getNomeComprador().isBlank()) {
            throw new IllegalArgumentException("Informe o nome do comprador.");
        }

        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new IllegalArgumentException("Seu carrinho está vazio.");
        }

        Map<Long, Integer> quantidadePorPresente = new HashMap<>();

        for (CompraCarrinhoRequest.ItemCarrinho item : request.getItens()) {
            if (item.getPresenteId() == null) {
                throw new IllegalArgumentException("Item do carrinho inválido.");
            }
            if (item.getQuantidade() == null || item.getQuantidade() < 1) {
                throw new IllegalArgumentException("Quantidade inválida para um dos itens.");
            }
            quantidadePorPresente.merge(item.getPresenteId(), item.getQuantidade(), Integer::sum);
        }

        BigDecimal total = BigDecimal.ZERO;
        String nomeComprador = request.getNomeComprador().trim();

        for (Map.Entry<Long, Integer> entrada : quantidadePorPresente.entrySet()) {
            PresenteCasamento presente = presenteRepository.findById(entrada.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Presente não encontrado."));

            int quantidade = entrada.getValue();
            int disponiveis = presente.getCotasDisponiveis();

            if (quantidade > disponiveis) {
                throw new IllegalArgumentException(
                        "Não há cotas suficientes para \"" + presente.getNome() + "\". Disponível: " + disponiveis
                );
            }

            presente.setCotasVendidas(presente.getCotasVendidas() + quantidade);
            presente.atualizarStatusComprado();
            presente.setNomeComprador(nomeComprador);
            presenteRepository.save(presente);

            total = total.add(presente.getValor().multiply(BigDecimal.valueOf(quantidade)));
        }

        return new FinalizarCarrinhoResponse(
                total,
                "Obrigado pelo carinho! Suas cotas foram confirmadas com sucesso."
        );
    }
}
