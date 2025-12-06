package io.quarkus.builder.json;

/**
 * @deprecated since 3.31.0 in favor of {@link io.quarkus.bootstrap.json.JsonBoolean}
 */
@Deprecated(forRemoval = true)
public enum JsonBoolean implements JsonValue {
    TRUE(true),
    FALSE(false);

    private final boolean value;

    JsonBoolean(boolean value) {
        this.value = value;
    }

    public boolean value() {
        return value;
    }
}
