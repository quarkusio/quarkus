package io.quarkus.opentelemetry.runtime.config.build;

public enum SamplerType {
    /**
     * Samples all spans.
     */
    ALWAYS_ON(Constants.ALWAYS_ON),
    /**
     * Drops all spans.
     */
    ALWAYS_OFF(Constants.ALWAYS_OFF),
    /**
     * Samples spans based on a configurable ratio. The arg runtime config.
     */
    TRACE_ID_RATIO(Constants.TRACE_ID_RATIO),
    /**
     * Samples spans based on sampling status of the span’s parent.
     * If parent is sampled, then samples all spans.
     */
    PARENT_BASED_ALWAYS_ON(Constants.PARENT_BASED_ALWAYS_ON),
    /**
     * Drops all spans
     */
    PARENT_BASED_ALWAYS_OFF(Constants.PARENT_BASED_ALWAYS_OFF),
    /**
     * Samples spans based on sampling status of the span’s parent.
     * If parent is sampled, samples spans based on a configurable ratio.
     */
    PARENT_BASED_TRACE_ID_RATIO(Constants.PARENT_BASED_TRACE_ID_RATIO);

    private final String value;

    SamplerType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    static class Constants {
        public static final String ALWAYS_ON = "always_on";
        public static final String ALWAYS_OFF = "always_off";
        public static final String TRACE_ID_RATIO = "traceidratio";
        public static final String PARENT_BASED_ALWAYS_ON = "parentbased_always_on";
        public static final String PARENT_BASED_ALWAYS_OFF = "parentbased_always_off";
        public static final String PARENT_BASED_TRACE_ID_RATIO = "parentbased_traceidratio";
    }
}
