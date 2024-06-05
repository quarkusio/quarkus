package io.quarkus.resteasy.reactive.jackson.runtime.security;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class RolesAllowedConfigExpStorage {

    private final Map<String, Supplier<String[]>> configExpToAllowedRoles;
    private final Map<String, String[]> rolesAllowedExpCache;

    public RolesAllowedConfigExpStorage(Map<String, Supplier<String[]>> configExpToAllowedRoles) {
        this.configExpToAllowedRoles = Map.copyOf(configExpToAllowedRoles);
        this.rolesAllowedExpCache = new HashMap<>();
    }

    /**
     * Transforms configuration expressions to configuration values.
     * Should be called on startup once runtime config is ready.
     */
    public synchronized void resolveRolesAllowedConfigExp() {
        if (rolesAllowedExpCache.isEmpty()) {
            for (Map.Entry<String, Supplier<String[]>> e : configExpToAllowedRoles.entrySet()) {
                String roleConfigExp = e.getKey();
                Supplier<String[]> rolesSupplier = e.getValue();
                rolesAllowedExpCache.put(roleConfigExp, rolesSupplier.get());
            }
        }
    }

    public String[] getRoles(String configExpression) {
        return rolesAllowedExpCache.get(configExpression);
    }
}
