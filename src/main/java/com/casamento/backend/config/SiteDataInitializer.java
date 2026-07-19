package com.casamento.backend.config;

import com.casamento.backend.model.Site;
import com.casamento.backend.repository.SiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;

@Component
public class SiteDataInitializer implements CommandLineRunner {

    private static final String VERSICULO_PADRAO =
            "\"Assim, eles já não são dois, mas sim uma só carne. Portanto, o que Deus uniu, ninguém separe.\" Mateus 19:6";

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private Environment environment;

    @Override
    public void run(String... args) {
        if (Arrays.asList(environment.getActiveProfiles()).contains("test")) {
            return;
        }

        garantirRafaekevin();
        garantirModelo();
    }

    /** Site vitrine (demo de venda) — só cria se ainda não existir. */
    private void garantirRafaekevin() {
        if (siteRepository.findBySlug("rafaekevin").isPresent()) {
            return;
        }

        Site site = new Site();
        site.setSlug("rafaekevin");
        site.setNomeNoiva("Rafaella");
        site.setNomeNoivo("Kevin");
        site.setNomeCurto("Rafa & Kevin");
        site.setDataCasamento(LocalDate.of(2027, 4, 24));
        site.setHoraCasamento("16:00");
        site.setDiaSemana("SÁBADO");
        site.setMesExtenso("ABRIL");
        site.setVersiculo(VERSICULO_PADRAO);
        site.setFraseBencao("Com a bênção de Deus e nossos pais");
        site.setTituloGaleria("Nossos momentos");
        site.setHistoriaCurta("Um encontro, um sim e o começo da nossa história juntos.");
        site.setAtivo(true);
        site.setAssinaturaStatus("ATIVA");
        siteRepository.save(site);
    }

    /**
     * Template em branco para a noiva ver o layout sem fotos do casal demo.
     * Sem imagens: o front mostra placeholders.
     */
    private void garantirModelo() {
        if (siteRepository.findBySlug("modelo").isPresent()) {
            return;
        }

        Site site = new Site();
        site.setSlug("modelo");
        site.setNomeNoiva("Noiva");
        site.setNomeNoivo("Noivo");
        site.setNomeCurto("Seu casamento");
        site.setVersiculo(VERSICULO_PADRAO);
        site.setFraseBencao("Com a bênção de Deus e nossos pais");
        site.setTituloGaleria("Nossos momentos");
        site.setHistoriaCurta("Conte em poucas linhas como vocês se encontraram.");
        site.setPaisNoiva("Pais da noiva");
        site.setPaisNoivo("Pais do noivo");
        site.setLocalNome("Local da celebração");
        site.setAtivo(true);
        site.setAssinaturaStatus("ATIVA");
        siteRepository.save(site);
    }
}
