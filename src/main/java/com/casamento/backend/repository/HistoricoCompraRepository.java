package com.casamento.backend.repository;

import com.casamento.backend.model.HistoricoCompraCota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoCompraRepository extends JpaRepository<HistoricoCompraCota, Long> {

    List<HistoricoCompraCota> findAllByOrderByDataCompraDesc();
}
