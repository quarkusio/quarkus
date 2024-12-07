package io.quarkus.oidc.common;

import java.util.Collections;
import java.util.HashMap;
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
        this.properties = new HashMap<>(properties);
    }

    /**
     * Get property value
     *
     * @param name property name
     * @return property value
     */
    public <T> T get(String name) {
        @SuppressWarnings("unchecked")
        T value = (T) properties.get(name);
        return value;
    }

    /**
     * Get property value as String
     *
     * @param name property name
     * @return property value as String
     */
    public String getString(String name) {
        return (String) get(name);
    }

    /**
     * Get typed property value
     *
     * @param name property name
     * @param type property type
     * @return typed property value
     */
    public <T> T get(String name, Class<T> type) {
        return type.cast(get(name));
    }

    /**
     * Get an unmodifiable view of the current context properties.
     *
     * @return all properties
     */
    public Map<String, Object> getAll() {
        return Collections.unmodifiableMap(properties);
    }

    /**
     * Set the property
     *
     * @param name property name
     * @param value property value
     * @return this OidcRequestContextProperties instance
     */
    public OidcRequestContextProperties put(String name, Object value) {
        properties.put(name, value);
        return this;
    }
}
