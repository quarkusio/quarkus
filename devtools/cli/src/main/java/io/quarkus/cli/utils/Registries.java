package io.quarkus.cli.utils;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import io.quarkus.cli.registry.RegistryClientMixin;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.config.RegistryConfig;

public final class Registries {

    private Registries() {
        // Utility class
    }

    public static Set<String> getRegistries(RegistryClientMixin client, String... additionalRegistires) {
        Set<String> registries = new LinkedHashSet<>();
        try {
            for (RegistryConfig c : client.resolveConfig().getRegistries()) {
                registries.add(c.getId());
            }
            for (String r : additionalRegistires) {
                registries.add(r);
            }
            return registries;
        } catch (RegistryResolutionException e) {
            return new HashSet<>();
        }
    }
}
