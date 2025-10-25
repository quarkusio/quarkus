package io.quarkus.websockets.next.deployment;

public final class WebSocketConstants {

    /**
     * Hardcoding the "SpanAttribute" annotation name so that we don't have to introduce a deployment module SPI.
     * There is a test that fails if this name ever changes.
     */
    public static final String OPEN_TELEMETRY_SPAN_ATTRIBUTE = "io.opentelemetry.instrumentation.annotations.SpanAttribute";

}
