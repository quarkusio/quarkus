package io.quarkus.builder.json;

public final class JsonDouble implements JsonNumber {
    private final double value;

    public JsonDouble(double value) {
        this.value = value;
    }

    public double value() {
        return value;
    }
}
