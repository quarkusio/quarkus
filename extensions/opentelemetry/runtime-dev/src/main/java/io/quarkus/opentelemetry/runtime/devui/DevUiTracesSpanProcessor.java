package io.quarkus.opentelemetry.runtime.devui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.ServiceAttributes;
import io.quarkus.dev.telemetry.TelemetryEvent;
import io.quarkus.dev.telemetry.TelemetryEvents;

/**
 * Sends every finished span to the Dev UI traces view, as a {@code span} telemetry event. It knows nothing of how
 * the spans are kept or shown; that belongs to the core Dev UI.
 * <p>
 * NOTE: onEnd runs on the span-completion path (often the request thread), so the conversion deliberately avoids
 * streams and lambdas to keep the capture path lean. Plain loops only here. For the same reason nothing may escape it:
 * the SDK does not guard {@code span.end()}, so an exception here would fail the application's request and keep the
 * span from the processors after this one. Dev mode tooling must never break the application.
 */
public class DevUiTracesSpanProcessor implements SpanProcessor {

    private static final Logger LOG = Logger.getLogger(DevUiTracesSpanProcessor.class);

    /** What the OpenTelemetry SDK names a span that was started without a name. */
    static final String UNNAMED_SPAN = "<unspecified span name>";

    private final Event<TelemetryEvent> telemetry;

    @Inject
    public DevUiTracesSpanProcessor(Event<TelemetryEvent> telemetry) {
        this.telemetry = telemetry;
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        // no-op: capture happens on end
    }

    @Override
    public boolean isStartRequired() {
        return false;
    }

    @Override
    public void onEnd(ReadableSpan span) {
        if (TelemetryEvents.isEnabled()) {
            try {
                telemetry.fire(toEvent(span.toSpanData()));
            } catch (RuntimeException e) {
                LOG.debugf(e, "Could not send span %s to the Dev UI", span.getName());
            }
        }
    }

    @Override
    public boolean isEndRequired() {
        return true;
    }

    static TelemetryEvent toEvent(SpanData data) {
        Map<String, String> attributes = new LinkedHashMap<>();
        for (Map.Entry<AttributeKey<?>, Object> entry : data.getAttributes().asMap().entrySet()) {
            attributes.put(entry.getKey().getKey(), String.valueOf(entry.getValue()));
        }
        List<EventData> spanEvents = data.getEvents();
        List<String> events = new ArrayList<>(spanEvents.size());
        for (EventData event : spanEvents) {
            events.add(event.getName() + " @" + event.getEpochNanos());
        }
        String serviceName = data.getResource().getAttribute(ServiceAttributes.SERVICE_NAME);
        // A span is only named when it starts, but it can be renamed to nothing afterwards; the SDK does not stop it.
        String name = data.getName();
        return TelemetryEvent.span(name == null || name.isBlank() ? UNNAMED_SPAN : name)
                .traceId(data.getTraceId())
                .spanId(data.getSpanId())
                .parentSpanId(data.getParentSpanId())
                .kind(data.getKind().name())
                .time(data.getStartEpochNanos(), data.getEndEpochNanos())
                .status(data.getStatus().getStatusCode().name(), data.getStatus().getDescription())
                .scope(data.getInstrumentationScopeInfo().getName())
                .serviceName(serviceName == null ? "" : serviceName)
                .spanAttributes(attributes)
                .events(events)
                .build();
    }
}
