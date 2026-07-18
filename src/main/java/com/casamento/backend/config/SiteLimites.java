package com.casamento.backend.config;

import com.casamento.backend.model.Site;

/**
 * Limites comerciais por casamento (SaaS).
 * O site modelo (rafaekevin) fica sem teto de presentes.
 */
public final class SiteLimites {

    public static final int MAX_PRESENTES_CLIENTE = 20;
    public static final int MAX_FOTOS_CARROSSEL = 6;

    /** Slug do nosso casamento-modelo — sem limite de presentes. */
    public static final String SLUG_MODELO = "rafaekevin";

    private SiteLimites() {
    }

    public static boolean semLimitePresentes(Site site) {
        return site != null && SLUG_MODELO.equalsIgnoreCase(site.getSlug());
    }

    public static int maxPresentes(Site site) {
        return semLimitePresentes(site) ? Integer.MAX_VALUE : MAX_PRESENTES_CLIENTE;
    }
}
