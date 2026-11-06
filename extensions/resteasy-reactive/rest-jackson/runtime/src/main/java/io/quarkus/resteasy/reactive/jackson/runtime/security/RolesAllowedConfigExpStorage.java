package io.quarkus.resteasy.reactive.jackson.runtime.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class RolesAllowedConfigExpStorage implements RolesAllowedConfigExpConsumer {

    private final Map<String, Supplier<String[]>> configExpToAllowedRoles = new ConcurrentHashMap<>();
    private final Map<String, String[]> rolesAllowedExpCache = new ConcurrentHashMap<>();

    @Override
    public void accept(String configExpression, Supplier<String[]> rolesSupplier) {
        configExpToAllowedRoles.put(configExpression, rolesSupplier);
    }

    /**
     * Transforms configuration expressions to configuration values.
     * Should be called on startup once runtime config is ready.
     */
    public synchronized void resolveRolesAllowedConfigExp() {
        if (rolesAllowedExpCache.isEmpty()) {
            if (configExpToAllowedRoles.isEmpty()) {
                // there is no reason why this should happen, because the expressions are registered
                // during static-init, before runtime configuration is ready
                throw new IllegalStateException(
                        "No roles allowed configuration expression was registered before runtime configuration became ready");
            }
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
