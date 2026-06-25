package com.casamento.backend.config;

import com.casamento.backend.model.AdminUsuario;
import com.casamento.backend.repository.AdminUsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class AdminDataInitializer implements CommandLineRunner {

    @Autowired
    private AdminUsuarioRepository adminUsuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Environment environment;

    @Value("${admin.default-login}")
    private String defaultLogin;

    @Value("${admin.default-password}")
    private String defaultPassword;

    @Override
    public void run(String... args) {
        if (Arrays.asList(environment.getActiveProfiles()).contains("test")) {
            return;
        }

        String login = defaultLogin.trim();

        AdminUsuario admin = adminUsuarioRepository.findByLogin(login)
                .orElseGet(AdminUsuario::new);

        admin.setLogin(login);
        admin.setSenhaHash(passwordEncoder.encode(defaultPassword));
        adminUsuarioRepository.save(admin);
    }
}
