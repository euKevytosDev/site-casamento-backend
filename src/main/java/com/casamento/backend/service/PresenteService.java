package com.casamento.backend.service;

import com.casamento.backend.dto.CompraCarrinhoRequest;
import com.casamento.backend.dto.FinalizarCarrinhoResponse;
import com.casamento.backend.dto.GerarPixResponse;
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

    @Autowired
    private PixPayloadService pixPayloadService;

    public GerarPixResponse gerarPix(CompraCarrinhoRequest request) {
        BigDecimal total = calcularTotalSemReservar(request.getItens());
        String txid = pixPayloadService.gerarTxid();
        String pixCopiaCola = pixPayloadService.gerarPayload(total, txid);

        return new GerarPixResponse(
                total,
                pixCopiaCola,
                txid,
                "Escaneie o QR Code ou copie o código PIX para pagar."
        );
    }

    @Transactional
    public FinalizarCarrinhoResponse finalizarCarrinho(CompraCarrinhoRequest request) {
        if (request.getNomeComprador() == null || request.getNomeComprador().isBlank()) {
            throw new IllegalArgumentException("Informe o nome do comprador.");
        }

        Map<Long, Integer> quantidadePorPresente = agruparItens(request.getItens());
        BigDecimal total = reservarCotas(quantidadePorPresente, request.getNomeComprador().trim());

        return new FinalizarCarrinhoResponse(
                total,
                "Obrigado pelo carinho! Suas cotas foram confirmadas com sucesso."
        );
    }

    private Map<Long, Integer> agruparItens(java.util.List<CompraCarrinhoRequest.ItemCarrinho> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("Seu carrinho está vazio.");
        }

        Map<Long, Integer> quantidadePorPresente = new HashMap<>();

        for (CompraCarrinhoRequest.ItemCarrinho item : itens) {
            if (item.getPresenteId() == null) {
                throw new IllegalArgumentException("Item do carrinho inválido.");
            }
            if (item.getQuantidade() == null || item.getQuantidade() < 1) {
                throw new IllegalArgumentException("Quantidade inválida para um dos itens.");
            }
            quantidadePorPresente.merge(item.getPresenteId(), item.getQuantidade(), Integer::sum);
        }

        return quantidadePorPresente;
    }

    private BigDecimal calcularTotalSemReservar(java.util.List<CompraCarrinhoRequest.ItemCarrinho> itens) {
        Map<Long, Integer> quantidadePorPresente = agruparItens(itens);
        return calcularTotal(quantidadePorPresente);
    }

    private BigDecimal calcularTotal(Map<Long, Integer> quantidadePorPresente) {
        BigDecimal total = BigDecimal.ZERO;

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

            total = total.add(presente.getValor().multiply(BigDecimal.valueOf(quantidade)));
        }

        return total;
    }

    private BigDecimal reservarCotas(Map<Long, Integer> quantidadePorPresente, String nomeComprador) {
        BigDecimal total = BigDecimal.ZERO;

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

        return total;
    }
}
