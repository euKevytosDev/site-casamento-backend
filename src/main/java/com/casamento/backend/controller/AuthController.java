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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    @Transactional(readOnly = true)
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

    /**
     * Recuperação sem SMTP: e-mail da conta + slug do site (prova de posse).
     * Body: { email, slug, novaSenha }
     */
    @Transactional
    @PostMapping("/recuperar-senha")
    public ResponseEntity<?> recuperarSenha(@RequestBody Map<String, String> body) {
        String email = str(body.get("email")).toLowerCase();
        String slug = normalizarSlug(str(body.get("slug")));
        String novaSenha = body.get("novaSenha") == null ? "" : body.get("novaSenha");

        if (email.isBlank() || !email.contains("@") || slug.isBlank()) {
            return ResponseEntity.badRequest().body("Informe o e-mail da conta e o link do site.");
        }
        if (novaSenha.length() < 6) {
            return ResponseEntity.badRequest().body("A nova senha precisa ter pelo menos 6 caracteres.");
        }

        UsuarioNoiva usuario = usuarioNoivaRepository.findByEmailIgnoreCase(email).orElse(null);
        if (usuario == null || usuario.getSite() == null
                || !slug.equalsIgnoreCase(usuario.getSite().getSlug())) {
            // Mensagem genérica — não revela se o e-mail existe
            return ResponseEntity.status(400).body("E-mail e link do site não conferem. Confira e tente de novo.");
        }

        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuarioNoivaRepository.save(usuario);

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "mensagem", "Senha atualizada. Você já pode entrar no painel."
        ));
    }

    private static String str(String v) {
        return v == null ? "" : v.trim();
    }

    private static String normalizarSlug(String slug) {
        if (slug == null) return "";
        String s = slug.trim().toLowerCase();
        // Aceita URL colada: .../?site=foo ou .../foo
        int idx = s.indexOf("site=");
        if (idx >= 0) {
            s = s.substring(idx + 5);
            int amp = s.indexOf('&');
            if (amp >= 0) s = s.substring(0, amp);
            int hash = s.indexOf('#');
            if (hash >= 0) s = s.substring(0, hash);
        }
        return s.replaceAll("[^a-z0-9-]", "");
    }
}
