package com.casamento.backend.repository;

import com.casamento.backend.model.RecadoCasamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecadoRepository extends JpaRepository<RecadoCasamento, Long> {
    List<RecadoCasamento> findBySiteIdOrderByDataCadastroDesc(Long siteId);
    long countBySiteId(Long siteId);
}
