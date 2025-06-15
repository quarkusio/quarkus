package io.quarkus.redis.datasource.search;

import java.util.Collections;
import java.util.Map;

/**
 * Represents a document from a {@code ft.aggregate} response.
 */
public class AggregateDocument {

    private final Map<String, Document.Property> properties;

    public AggregateDocument(Map<String, Document.Property> properties) {
        this.properties = properties == null ? Collections.emptyMap() : Collections.unmodifiableMap(properties);
    }

    /**
     * @return the document properties
     */
    public Map<String, Document.Property> properties() {
        return properties;
    }

    /**
     * Gets a property from the document.
     *
     * @param name
     *        the property name, must not be {@code null}
     *
     * @return the property, {@code null} if not found
     */
    public Document.Property property(String name) {
        return properties.get(name);
    }

}
