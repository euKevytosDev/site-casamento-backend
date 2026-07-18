package com.casamento.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Conta da noiva/casal para acessar o painel do próprio site.
 */
@Entity
@Table(name = "usuario_noiva")
public class UsuarioNoiva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    public UsuarioNoiva() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }

    public Site getSite() { return site; }
    public void setSite(Site site) { this.site = site; }

    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
