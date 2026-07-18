package com.casamento.backend.repository;

import com.casamento.backend.model.UsuarioNoiva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioNoivaRepository extends JpaRepository<UsuarioNoiva, Long> {
    Optional<UsuarioNoiva> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
