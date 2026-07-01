package com.casamento.backend.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Configuration
public class SecretsValidator {

    private static final String[] SEGREDOS_OBRIGATORIOS = {
            "DB_URL", "DB_USERNAME", "DB_PASSWORD", "JWT_SECRET", "ADMIN_LOGIN", "ADMIN_PASSWORD",
            "CLOUDINARY_CLOUD_NAME", "CLOUDINARY_API_KEY", "CLOUDINARY_API_SECRET"
    };

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${admin.default-password:}")
    private String adminPassword;

    private final Environment environment;

    public SecretsValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validar() {
        if (Arrays.asList(environment.getActiveProfiles()).contains("test")) {
            return;
        }

        for (String segredo : SEGREDOS_OBRIGATORIOS) {
            String valor = environment.getProperty(segredo);
            if (valor == null || valor.isBlank()) {
                throw new IllegalStateException(
                        "Variável obrigatória não configurada: " + segredo
                                + ". Configure no Render ou em application-local.properties."
                );
            }
        }

        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET deve ter pelo menos 32 caracteres.");
        }

        if (adminPassword.length() < 8) {
            throw new IllegalStateException("ADMIN_PASSWORD deve ter pelo menos 8 caracteres.");
        }
    }
}
