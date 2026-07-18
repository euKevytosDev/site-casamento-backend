package com.casamento.backend.repository;

import com.casamento.backend.model.PedidoPresente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PedidoPresenteRepository extends JpaRepository<PedidoPresente, Long> {
    Optional<PedidoPresente> findByMpPreferenceId(String mpPreferenceId);
    Optional<PedidoPresente> findByMpPaymentId(String mpPaymentId);
}
