package io.quarkus.oidc.common;

import java.util.Map;

public class OidcRequestContextProperties {

    public static String TOKEN = "token";
    public static String TOKEN_CREDENTIAL = "token_credential";

    private final Map<String, Object> properties;

    public OidcRequestContextProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public Object get(String name) {
        return properties.get(name);
    }

    public String getString(String name) {
        return (String) get(name);
    }

    public <T> T get(String name, Class<T> type) {
        return type.cast(get(name));
    }

}
