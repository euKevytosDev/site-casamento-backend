package com.casamento.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "presenca_casamento") // Avisa ao Java o nome EXATO da tabela que criamos no Postgres

public class PresencaCasamento {

    @Id // Diz que este campo é a chave primária (o identificador único)
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Avisa que o banco gera o número sozinho (nosso Serial)
    private Long id;

    // Conecta com a coluna 'nome_convidado' e diz que ela não pode ser vazia
    @Column(name = "nome_convidado", nullable = false)
    private String nome;

    // Conecta com a coluna 'idade' (usamos Integer porque aceita números inteiros)
    @Column(name = "idade")
    private Integer idade;

    // Conecta com a coluna 'confirmado' (TRUE ou FALSE)
    @Column(name = "confirmado", nullable = false)
    private Boolean confirmado;

    // Conecta com a data de cadastro. O 'insertable = false' avisa ao Java para deixar o Postgres colocar a data sozinho
    @Column(name = "data_cadastro", insertable = false, updatable = false)
    private LocalDateTime dataCadastro;

    // Construtor vazio (obrigatório Spring)
    public PresencaCasamento(){

    }

    // abaixo metodos getters and setters (Os "porteiros" da classe)
    // O Java precisa deles para ler (Get) e gravar (Set) as informações dentro de cada campo.

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() { return nome; }

    public void setNome(String nome) { this.nome = nome; }

    public Integer getIdade() {
        return idade;
    }

    public void setIdade(Integer idade) {
        this.idade = idade;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDateTime dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public Boolean getConfirmado() {
        return confirmado;
    }

    public void setConfirmado(Boolean confirmado) {
        this.confirmado = confirmado;
    }
}