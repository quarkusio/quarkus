package io.quarkus.opentelemetry.runtime.config.runtime.exporter;

public class OtelConnectionRuntimeConfig {

    // In the future this class will be reused by metrics and logs. Will hold the default properties.

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

    public static class Constants {
        public static final String DEFAULT_TIMEOUT_SECS = "10";
    }
}
