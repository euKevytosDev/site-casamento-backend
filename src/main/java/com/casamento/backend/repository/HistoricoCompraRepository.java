package com.casamento.backend.repository;

import com.casamento.backend.model.HistoricoCompraCota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HistoricoCompraRepository extends JpaRepository<HistoricoCompraCota, Long> {
    @Query("""
        SELECT h FROM HistoricoCompraCota h
        WHERE h.presenteId IN (
            SELECT p.id FROM PresenteCasamento p WHERE p.site.id = :siteId
        )
        ORDER BY h.dataCompra DESC
        """)
    List<HistoricoCompraCota> findBySiteIdOrderByDataCompraDesc(@Param("siteId") Long siteId);

}
