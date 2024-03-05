package io.quarkus.builder.json;

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
