package io.quarkus.oidc.common;

import java.util.Collections;
import java.util.Map;

public class OidcRequestContextProperties {

    public static String TOKEN = "token";
    public static String TOKEN_CREDENTIAL = "token_credential";
    public static String DISCOVERY_ENDPOINT = "discovery_endpoint";

    private final Map<String, Object> properties;

    public OidcRequestContextProperties() {
        this(Map.of());
    }

    public OidcRequestContextProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public <T> T get(String name) {
        @SuppressWarnings("unchecked")
        T value = (T) properties.get(name);
        return value;
    }

    public String getString(String name) {
        return (String) get(name);
    }

    public <T> T get(String name, Class<T> type) {
        return type.cast(get(name));
    }

    public Map<String, Object> getAll() {
        return Collections.unmodifiableMap(properties);
    }

}
