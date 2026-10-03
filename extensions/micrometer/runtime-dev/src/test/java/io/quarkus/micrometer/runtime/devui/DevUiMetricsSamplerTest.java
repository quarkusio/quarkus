package io.quarkus.micrometer.runtime.devui;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.NotificationOptions;
import jakarta.enterprise.util.TypeLiteral;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.dev.telemetry.TelemetryEvent;
import io.quarkus.dev.telemetry.TelemetryEvents;

public class DevUiMetricsSamplerTest {

    @BeforeEach
    void enable() {
        TelemetryEvents.setEnabled(true);
    }

    @AfterEach
    void disable() {
        TelemetryEvents.setEnabled(false);
    }

    @Test
    public void aMeterThatCannotBeSentDoesNotCostTheOthersTheirReading() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        registry.counter("before").increment();
        registry.counter("boom").increment();
        registry.counter(" ").increment();
        registry.counter("after").increment();

        DevUiMetricsSampler sampler = new DevUiMetricsSampler();
        sampler.registry = registry;
        RecordingEvent telemetry = new RecordingEvent("boom");
        sampler.telemetry = telemetry;

        sampler.sample();

        // The meters come in no particular order, so whichever of them comes after the failing one is still sent. A
        // meter without a name has nothing to be charted under and is left out.
        assertThat(telemetry.names).containsExactlyInAnyOrder("before", "after");
    }

    /** Receives events like the Dev UI does, failing on one of them the way a broken receiver would. */
    static final class RecordingEvent implements Event<TelemetryEvent> {

        final List<String> names = new ArrayList<>();
        private final String failOn;

        RecordingEvent(String failOn) {
            this.failOn = failOn;
        }

        @Override
        public void fire(TelemetryEvent event) {
            if (event.name().equals(failOn)) {
                throw new IllegalStateException("receiver failed");
            }
            names.add(event.name());
        }

        @Override
        public <U extends TelemetryEvent> CompletionStage<U> fireAsync(U event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <U extends TelemetryEvent> CompletionStage<U> fireAsync(U event, NotificationOptions options) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Event<TelemetryEvent> select(Annotation... qualifiers) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <U extends TelemetryEvent> Event<U> select(Class<U> subtype, Annotation... qualifiers) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <U extends TelemetryEvent> Event<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
            throw new UnsupportedOperationException();
        }
    }
}
