package io.quarkus.opentelemetry.runtime.config.build;

public enum ExporterType {
    OTLP(Constants.OTLP_VALUE),
    HTTP(Constants.HTTP_VALUE),
    //    JAEGER(Constants.JAEGER), // Moved to Quarkiverse
    /**
     * To be used by legacy CDI beans setup. Will be removed soon.
     */
    CDI(Constants.CDI_VALUE),
    NONE(Constants.NONE_VALUE);

    private final String value;

    ExporterType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static class Constants {
        public static final String OTLP_VALUE = "otlp";
        public static final String CDI_VALUE = "cdi";
        public static final String HTTP_VALUE = "http";
        public static final String NONE_VALUE = "none";
        public static final String JAEGER = "jaeger";
    }
}
