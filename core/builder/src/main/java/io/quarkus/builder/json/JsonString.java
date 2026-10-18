package io.quarkus.builder.json;

import java.util.Objects;

/**
 * @deprecated since 3.31.0 in favor of {@link io.quarkus.bootstrap.json.JsonString}
 */
@Deprecated(forRemoval = true)
public final class JsonString implements JsonValue {
    private final String value;

    public JsonString(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        JsonString that = (JsonString) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
