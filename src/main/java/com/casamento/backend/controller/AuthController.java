package com.casamento.backend.controller;

import com.casamento.backend.dto.LoginRequest;
import com.casamento.backend.dto.LoginResponse;
import com.casamento.backend.model.AdminUsuario;
import com.casamento.backend.repository.AdminUsuarioRepository;
import com.casamento.backend.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AdminUsuarioRepository adminUsuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.getLogin() == null || request.getLogin().isBlank()
                || request.getSenha() == null || request.getSenha().isBlank()) {
            return ResponseEntity.badRequest().body("Informe login e senha.");
        }

        AdminUsuario admin = adminUsuarioRepository.findByLogin(request.getLogin().trim())
                .orElse(null);

        if (admin == null || !passwordEncoder.matches(request.getSenha(), admin.getSenhaHash())) {
            return ResponseEntity.status(401).body("Login ou senha inválidos.");
        }

        String token = jwtService.gerarToken(admin.getLogin());
        return ResponseEntity.ok(new LoginResponse(token, admin.getLogin()));
    }
}
