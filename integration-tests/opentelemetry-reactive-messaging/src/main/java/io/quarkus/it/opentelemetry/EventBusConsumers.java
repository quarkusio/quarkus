package io.quarkus.it.opentelemetry;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.vertx.ConsumeEvent;

/**
 * Event bus consumers used to exercise the OpenTelemetry Vert.x {@code EventBusInstrumenterVertxTracer}.
 *
 * @see EventBusResource
 */
@ApplicationScoped
public class EventBusConsumers {

    static final String PROPAGATE_ADDRESS = "eventbus-propagate";
    static final String ALWAYS_ADDRESS = "eventbus-always";

    @ConsumeEvent(PROPAGATE_ADDRESS)
    void onPropagate(String ignored) {
    }

    @ConsumeEvent(ALWAYS_ADDRESS)
    void onAlways(String ignored) {
    }
}
