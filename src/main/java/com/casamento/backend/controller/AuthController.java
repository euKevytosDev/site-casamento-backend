package com.casamento.backend.controller;

import com.casamento.backend.dto.LoginRequest;
import com.casamento.backend.dto.LoginResponse;
import com.casamento.backend.model.AdminUsuario;
import com.casamento.backend.model.UsuarioNoiva;
import com.casamento.backend.repository.AdminUsuarioRepository;
import com.casamento.backend.repository.UsuarioNoivaRepository;
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
    private UsuarioNoivaRepository usuarioNoivaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    /** Login do admin global (você). */
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

        String token = jwtService.gerarToken(admin.getLogin(), null, "ADMIN");
        return ResponseEntity.ok(new LoginResponse(token, admin.getLogin(), null, "ADMIN"));
    }

    /** Login da noiva (e-mail + senha do checkout). */
    @PostMapping("/login-noiva")
    public ResponseEntity<?> loginNoiva(@RequestBody LoginRequest request) {
        if (request.getLogin() == null || request.getLogin().isBlank()
                || request.getSenha() == null || request.getSenha().isBlank()) {
            return ResponseEntity.badRequest().body("Informe e-mail e senha.");
        }

        UsuarioNoiva usuario = usuarioNoivaRepository
                .findByEmailIgnoreCase(request.getLogin().trim())
                .orElse(null);

        if (usuario == null || !passwordEncoder.matches(request.getSenha(), usuario.getSenhaHash())) {
            return ResponseEntity.status(401).body("E-mail ou senha inválidos.");
        }

        var site = usuario.getSite();
        if (site == null) {
            return ResponseEntity.status(401).body("Conta sem site vinculado.");
        }

        String status = site.getAssinaturaStatus() == null ? "" : site.getAssinaturaStatus();
        if ("PENDENTE".equalsIgnoreCase(status)) {
            return ResponseEntity.status(402)
                    .body("Pagamento pendente. Conclua o checkout para liberar o painel.");
        }
        if ("ATRASADA".equalsIgnoreCase(status) || !site.isAtivo()) {
            return ResponseEntity.status(402)
                    .body("Assinatura em atraso. Regularize o pagamento para religar o site.");
        }

        String token = jwtService.gerarToken(usuario.getEmail(), site.getSlug(), "NOIVA");
        return ResponseEntity.ok(new LoginResponse(token, usuario.getEmail(), site.getSlug(), "NOIVA"));
    }
}
