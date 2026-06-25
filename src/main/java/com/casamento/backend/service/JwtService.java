package com.casamento.backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expiracaoMs;

    public JwtService(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.expiration-hours:24}") long expirationHours) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET não configurado. Adicione a variável JWT_SECRET no Render (mínimo 32 caracteres)."
            );
        }

        if (secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET deve ter pelo menos 32 caracteres. Configure uma chave mais longa no Render."
            );
        }

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMs = expirationHours * 60 * 60 * 1000;
    }

    public String gerarToken(String login) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expiracaoMs);

        return Jwts.builder()
                .subject(login)
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(secretKey)
                .compact();
    }

    public String extrairLogin(String token) {
        return extrairClaims(token).getSubject();
    }

    public boolean tokenValido(String token) {
        try {
            Claims claims = extrairClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
