package com.casamento.backend.config;

import com.casamento.backend.model.Site;
import com.casamento.backend.repository.SiteRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro multi-tenant: em CADA request HTTP, lê o header X-Site-Id (slug),
 * busca o Site no banco e guarda em SiteContext para os controllers usarem.
 *
 * Fluxo:
 * 1) Front manda header: X-Site-Id: rafaekevin
 * 2) Este filtro acha o Site e faz SiteContext.set(site)
 * 3) Controller usa SiteContext.get() + findBySiteId(...)
 * 4) No finally, limpa o SiteContext (evita vazar site entre requests)
 */
@Component
public class SiteFilter extends OncePerRequestFilter {

    /** Nome do header que o frontend deve enviar (valor = slug do site). */
    public static final String HEADER = "X-Site-Id";

    @Autowired
    private SiteRepository siteRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // 1) Lê o slug enviado pelo front (ex.: "rafaekevin")
            String slug = request.getHeader(HEADER);

            if (slug != null && !slug.isBlank()) {
                // 2) Busca no banco pelo slug
                // 3) Só aceita se estiver ativo
                // 4) Guarda na "gaveta" da request atual
                siteRepository.findBySlug(slug.trim())
                        .filter(Site::isAtivo)
                        .ifPresent(SiteContext::set);
            }

            // Continua a cadeia (chega no JwtAuthFilter / controllers)
            filterChain.doFilter(request, response);

        } finally {
            // SEMPRE limpa — thread pode ser reutilizada no próximo request
            SiteContext.clear();
        }
    }
}
