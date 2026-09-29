package io.quarkus.opentelemetry.runtime.devui;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.NotificationOptions;
import jakarta.enterprise.util.TypeLiteral;

import io.quarkus.dev.telemetry.TelemetryEvent;

/** Receives events like the Dev UI does, failing on one of them the way a broken receiver would. */
final class RecordingEvent implements Event<TelemetryEvent> {

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
