package com.casamento.backend.repository;

import com.casamento.backend.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repository = ponte entre Java e o banco para a tabela "site".
 *
 * JpaRepository já traz métodos prontos:
 * - save(), findById(), findAll(), deleteById() etc.
 *
 * A gente só declara métodos extras quando precisa de busca específica.
 */
public interface SiteRepository extends JpaRepository<Site, Long> {

    /**
     * Busca um Site pelo slug (ex.: "rafaekevin").
     * Spring Data monta o SQL sozinho pelo NOME do método:
     * findBy + Slug → SELECT * FROM site WHERE slug = ?
     *
     * Optional = pode existir ou não (evita NullPointerException).
     */
    Optional<Site> findBySlug(String slug);
}
