package io.quarkus.opentelemetry.runtime.config.runtime;

public enum ExemplarsFilterType {
    TRACE_BASED(Constants.TRACE_BASED),
    ALWAYS_ON(Constants.ALWAYS_ON),
    ALWAYS_OFF(Constants.ALWAYS_OFF);

    private final String value;

    ExemplarsFilterType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static class Constants {
        public static final String TRACE_BASED = "trace_based";
        public static final String ALWAYS_ON = "always_on";
        public static final String ALWAYS_OFF = "always_off";
    }
}
