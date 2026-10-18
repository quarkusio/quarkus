package io.quarkus.oidc.test;

import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class OidcConfigSource implements ConfigSource {

    @Override
    public Set<String> getPropertyNames() {
        return Set.of("oidc.client-id");
    }

    @Override
    public String getValue(String propertyName) {
        return "oidc.client-id".equals(propertyName) ? "quarkus-web-app" : null;
    }

    @Override
    public String getName() {
        return "oidc";
    }

}
