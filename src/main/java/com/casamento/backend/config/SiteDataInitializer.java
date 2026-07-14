package com.casamento.backend.config;

import com.casamento.backend.model.Site;
import com.casamento.backend.repository.SiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;

@Component  // Spring executa ao subir
public class SiteDataInitializer implements CommandLineRunner {

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private Environment environment;

    @Override
    public void run(String... args) {
        // Não roda nos testes automatizados
        if (Arrays.asList(environment.getActiveProfiles()).contains("test")) {
            return;
        }

        // Se já existe, não cria de novo
        if (siteRepository.findBySlug("rafaekevin").isPresent()) {
            return;
        }

        Site site = new Site();
        site.setSlug("rafaekevin");
        site.setNomeNoiva("Rafaella");
        site.setNomeNoivo("Kevin");
        site.setDataCasamento(LocalDate.of(2027, 4, 24));
        site.setAtivo(true);

        siteRepository.save(site);
        // Depois que salvar, o site ganha um id (1, 2, ...)
    }
}
