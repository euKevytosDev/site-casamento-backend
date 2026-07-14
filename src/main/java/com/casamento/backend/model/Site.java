package com.casamento.backend.model;
import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Entidade Site = 1 casamento / 1 cliente no banco.
 * No multi-tenant, cada "inquilino" do sistema é um Site
 * (ex.: rafaekevin, mariaejoao). Presentes, presença e PIX
 * depois vão apontar para um Site via site_id.
 */
@Entity // Diz ao Hibernate/JPA: "esta classe vira uma tabela no banco"
@Table(name = "site") // Nome EXATO da tabela no PostgreSQL
public class Site {

    // Chave primária: número único de cada linha (1, 2, 3...)
    @Id
    // O banco gera o id sozinho (SERIAL / IDENTITY) — não precisamos enviar
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identificador público amigável do site.
     * Ex.: "rafaekevin" — o front manda isso no header X-Site-Id.
     * unique = true → dois casamentos não podem ter o mesmo slug.
     */
    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    // Coluna no banco: nome_noiva (Java usa camelCase; Postgres usa snake_case)
    @Column(name = "nome_noiva", nullable = false)
    private String nomeNoiva;

    @Column(name = "nome_noivo", nullable = false)
    private String nomeNoivo;

    // Só a data do casamento (sem hora). Ex.: 2027-04-24
    @Column(name = "data_casamento")
    private LocalDate dataCasamento;

    // Se false, o site fica "desligado" (não atende API desse cliente)
    @Column(nullable = false)
    private boolean ativo = true;

    // Construtor vazio: o Spring/JPA precisa dele para criar o objeto
    public Site() {
    }

    // --- Getters e setters: o Java lê (get) e grava (set) cada campo ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getNomeNoiva() {
        return nomeNoiva;
    }

    public void setNomeNoiva(String nomeNoiva) {
        this.nomeNoiva = nomeNoiva;
    }

    public String getNomeNoivo() {
        return nomeNoivo;
    }

    public void setNomeNoivo(String nomeNoivo) {
        this.nomeNoivo = nomeNoivo;
    }

    public LocalDate getDataCasamento() {
        return dataCasamento;
    }

    public void setDataCasamento(LocalDate dataCasamento) {
        this.dataCasamento = dataCasamento;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
