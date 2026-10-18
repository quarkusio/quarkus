package io.quarkus.keycloak.admin.client.common.deployment;

import java.util.function.BooleanSupplier;

public class KeycloakAdminClientInjectionEnabled implements BooleanSupplier {

    KeycloakAdminClientBuildTimeConfig config;

    @Override
    public boolean getAsBoolean() {
        return config.enabled();
    }
}
