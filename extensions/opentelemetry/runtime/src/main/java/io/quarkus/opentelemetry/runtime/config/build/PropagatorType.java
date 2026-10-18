package io.quarkus.opentelemetry.runtime.config.build;

public enum PropagatorType {
    TRACE_CONTEXT(Constants.TRACE_CONTEXT),
    BAGGAGE(Constants.BAGGAGE),
    B3(Constants.B3),
    B3MULTI(Constants.B3MULTI),
    JAEGER(Constants.JAEGER),
    XRAY(Constants.XRAY),
    OT_TRACE(Constants.OT_TRACE);

    private final String value;

    PropagatorType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    static class Constants {
        public static final String TRACE_CONTEXT = "tracecontext";
        public static final String BAGGAGE = "baggage";
        public static final String B3 = "b3";
        public static final String B3MULTI = "b3multi";
        public static final String JAEGER = "jaeger";
        public static final String XRAY = "xray";
        public static final String OT_TRACE = "ottrace";
    }
}
