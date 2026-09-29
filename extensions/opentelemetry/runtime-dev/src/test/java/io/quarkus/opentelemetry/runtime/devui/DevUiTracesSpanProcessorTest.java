package io.quarkus.opentelemetry.runtime.devui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.quarkus.dev.telemetry.TelemetryEvents;

public class DevUiTracesSpanProcessorTest {

    @BeforeEach
    void enable() {
        TelemetryEvents.setEnabled(true);
    }

    @AfterEach
    void disable() {
        TelemetryEvents.setEnabled(false);
    }

    @Test
    public void aSpanRenamedToNothingIsSentUnderThePlaceholderName() {
        RecordingEvent telemetry = new RecordingEvent(null);
        Span span = tracer(telemetry).spanBuilder("named-for-now").startSpan();
        span.updateName(" ");

        assertThatCode(span::end).doesNotThrowAnyException();
        assertThat(telemetry.names).containsExactly(DevUiTracesSpanProcessor.UNNAMED_SPAN);
    }

    @Test
    public void aReceiverThatFailsDoesNotFailTheSpan() {
        Span span = tracer(new RecordingEvent("boom")).spanBuilder("boom").startSpan();

        assertThatCode(span::end).doesNotThrowAnyException();
    }

    private static Tracer tracer(RecordingEvent telemetry) {
        return SdkTracerProvider.builder()
                .addSpanProcessor(new DevUiTracesSpanProcessor(telemetry))
                .build()
                .get("test");
    }
}
