package io.quarkus.bootstrap.json;

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

    /**
     * Returns {@code "true"} or {@code "false"} (lowercase) matching JSON literal syntax.
     */
    @Override
    public String toString() {
        return Boolean.toString(value);
    }
}
