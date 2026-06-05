package com.casamento.backend.repository;

import com.casamento.backend.model.PresencaCasamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository // Diz ao Spring que este arquivo é o responsável por mexer no banco de dados
public interface PresencaRepository extends JpaRepository<PresencaCasamento, Long> {

    // Sim, o arquivo fica VAZIO assim mesmo por dentro!
    // A mágica está no 'extends JpaRepository'.
    // Nós passamos para ele: <Qual é a Entidade, Qual é o tipo de dado do ID da Entidade>
    // Com isso, o Spring cria todos os comandos SQL por trás dos panos para nós.
}