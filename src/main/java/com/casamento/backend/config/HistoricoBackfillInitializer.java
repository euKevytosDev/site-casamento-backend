package com.casamento.backend.config;

import com.casamento.backend.model.HistoricoCompraCota;
import com.casamento.backend.model.PresenteCasamento;
import com.casamento.backend.repository.HistoricoCompraRepository;
import com.casamento.backend.repository.PresenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;

@Component
public class HistoricoBackfillInitializer implements CommandLineRunner {

    @Autowired
    private HistoricoCompraRepository historicoCompraRepository;

    @Autowired
    private PresenteRepository presenteRepository;

    @Autowired
    private Environment environment;

    @Override
    public void run(String... args) {
        if (Arrays.asList(environment.getActiveProfiles()).contains("test")) {
            return;
        }

        if (historicoCompraRepository.count() > 0) {
            return;
        }

        for (PresenteCasamento presente : presenteRepository.findAll()) {
            int vendidas = presente.getCotasVendidas() != null ? presente.getCotasVendidas() : 0;
            String comprador = presente.getNomeComprador();

            if (vendidas > 0 && comprador != null && !comprador.isBlank()) {
                HistoricoCompraCota historico = new HistoricoCompraCota();
                historico.setPresenteId(presente.getId());
                historico.setNomePresente(presente.getNome());
                historico.setNomeComprador(comprador.trim());
                historico.setQuantidade(vendidas);
                historico.setValorCota(presente.getValor());
                historico.setValorTotal(presente.getValor().multiply(BigDecimal.valueOf(vendidas)));
                historico.setDataCompra(presente.getDataCadastro());
                historicoCompraRepository.save(historico);
            }
        }
    }
}
