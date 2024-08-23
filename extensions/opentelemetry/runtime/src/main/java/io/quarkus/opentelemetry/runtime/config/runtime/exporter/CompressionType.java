package io.quarkus.opentelemetry.runtime.config.runtime.exporter;

public enum CompressionType {
    GZIP("gzip"),
    NONE("none");

    private final String value;

    CompressionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
