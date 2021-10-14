package io.quarkus.registry.config.json;

@Deprecated
public class JsonBooleanTrueFilter {

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Boolean)) {
            return false;
        }
        return (Boolean) obj;
    }
}