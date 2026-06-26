package io.quarkus.opentelemetry.observation.handler;

public final class ObservationHandlerPriorities {

    public static final int PROPAGATING_RECEIVER = 5000;
    public static final int PROPAGATING_SENDER = 4000;
    public static final int DEFAULT_TRACING = 3000;
    public static final int METER_TRACING_AWARE = 1000;

    private ObservationHandlerPriorities() {
    }
}
