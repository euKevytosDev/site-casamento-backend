package com.casamento.backend.config;

import com.casamento.backend.model.Site;

public final class SiteContext {
    private static final ThreadLocal<Site> ATUAL = new ThreadLocal<>();

    private SiteContext() {}

    public static void set(Site site) { ATUAL.set(site); }
    public static Site get() { return ATUAL.get(); }
    public static void clear() { ATUAL.remove(); }
}