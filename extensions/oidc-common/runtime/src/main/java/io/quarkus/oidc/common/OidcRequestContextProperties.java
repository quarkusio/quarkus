package io.quarkus.oidc.common;

import java.util.Map;

public class OidcRequestContextProperties {

    private final Map<String, Object> properties;

    public OidcRequestContextProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public Object getProperty(String name) {
        return properties.get(name);
    }
}
