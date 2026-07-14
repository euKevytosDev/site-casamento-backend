package com.casamento.backend.repository;
import java.util.List;
import com.casamento.backend.model.PresenteCasamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PresenteRepository extends JpaRepository<PresenteCasamento, Long> {

    List<PresenteCasamento> findBySiteId(Long siteId);
}
