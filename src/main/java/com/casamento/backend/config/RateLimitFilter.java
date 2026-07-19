package com.casamento.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limit simples em memória (por IP + rota) para login, checkout e webhooks.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L;

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        int limit = limitePara(method, path);

        if (limit > 0) {
            String key = clientIp(request) + "|" + method + "|" + normalizarRota(path);
            long agora = System.currentTimeMillis();
            Deque<Long> fila = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
            synchronized (fila) {
                while (!fila.isEmpty() && agora - fila.peekFirst() > WINDOW_MS) {
                    fila.pollFirst();
                }
                if (fila.size() >= limit) {
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                            "{\"error\":\"Muitas tentativas. Aguarde um minuto e tente de novo.\"}");
                    return;
                }
                fila.addLast(agora);
            }
        }

        filterChain.doFilter(request, response);
    }

    private static int limitePara(String method, String path) {
        if (!"POST".equalsIgnoreCase(method) && !"GET".equalsIgnoreCase(method)) {
            return 0;
        }
        if (path == null) return 0;

        if (path.equals("/api/auth/login") || path.equals("/api/auth/login-noiva")) {
            return 10;
        }
        if (path.equals("/api/assinatura/checkout")) {
            return 5;
        }
        if (path.startsWith("/api/webhooks/")) {
            return 60;
        }
        if (path.equals("/api/presenca/confirmar-familia")) {
            return 20;
        }
        if (path.equals("/api/recados") && "POST".equalsIgnoreCase(method)) {
            return 8;
        }
        if (path.equals("/api/presentes/finalizar-carrinho")
                || path.equals("/api/presentes/gerar-pix")
                || path.equals("/api/presentes/checkout-cartao")) {
            return 20;
        }
        return 0;
    }

    private static String normalizarRota(String path) {
        return path;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) {
            return real.trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }
}
