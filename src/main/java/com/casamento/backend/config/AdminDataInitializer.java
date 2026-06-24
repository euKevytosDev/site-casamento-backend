package com.casamento.backend.config;

import com.casamento.backend.model.AdminUsuario;
import com.casamento.backend.repository.AdminUsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminDataInitializer implements CommandLineRunner {

    @Autowired
    private AdminUsuarioRepository adminUsuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${admin.default-login:admin}")
    private String defaultLogin;

    @Value("${admin.default-password:casamento2027}")
    private String defaultPassword;

    @Override
    public void run(String... args) {
        if (adminUsuarioRepository.count() == 0) {
            AdminUsuario admin = new AdminUsuario();
            admin.setLogin(defaultLogin);
            admin.setSenhaHash(passwordEncoder.encode(defaultPassword));
            adminUsuarioRepository.save(admin);
        }
    }
}
