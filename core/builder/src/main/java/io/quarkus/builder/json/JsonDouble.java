package io.quarkus.builder.json;

/**
 * @deprecated since 3.31.0 in favor of {@link io.quarkus.bootstrap.json.JsonDouble}
 */
@Deprecated(forRemoval = true)
public final class JsonDouble implements JsonNumber {
    private final double value;

    public JsonDouble(double value) {
        this.value = value;
    }

    public double value() {
        return value;
    }
}
