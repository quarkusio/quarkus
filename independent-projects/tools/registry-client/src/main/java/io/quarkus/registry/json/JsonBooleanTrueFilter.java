package io.quarkus.registry.json;

/**
 * Serialization detail. Not part of the Catalog or Config API.
 */
public class JsonBooleanTrueFilter {
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Boolean)) {
            return false;
        }
        return (Boolean) obj;
    }
}
