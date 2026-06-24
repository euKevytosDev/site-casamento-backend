package com.casamento.backend.repository;

import com.casamento.backend.model.AdminUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminUsuarioRepository extends JpaRepository<AdminUsuario, Long> {

    Optional<AdminUsuario> findByLogin(String login);
}
