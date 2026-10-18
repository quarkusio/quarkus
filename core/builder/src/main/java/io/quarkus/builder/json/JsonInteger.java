package io.quarkus.builder.json;

/**
 * @deprecated since 3.31.0 in favor of {@link io.quarkus.bootstrap.json.JsonInteger}
 */
@Deprecated(forRemoval = true)
public final class JsonInteger implements JsonNumber {
    private final long value;

    public JsonInteger(long value) {
        this.value = value;
    }

    public long longValue() {
        return value;
    }

    public int intValue() {
        return (int) value;
    }
}
