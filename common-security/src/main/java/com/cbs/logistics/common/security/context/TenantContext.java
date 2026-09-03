package com.cbs.logistics.common.security.context;

/**
 * Contexte tenant ThreadLocal.
 *
 * <p>Le filtre {@link TenantFilter} extrait le claim {@code tenant_id}
 * du JWT et le place dans ce contexte. Toute la chaîne de traitement
 * (services, repositories) peut ensuite lire {@code TenantContext.getCurrent()}
 * pour filtrer les données par tenant.</p>
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setCurrent(String tenantId) {
        CURRENT.set(tenantId);
    }

    public static String getCurrent() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
