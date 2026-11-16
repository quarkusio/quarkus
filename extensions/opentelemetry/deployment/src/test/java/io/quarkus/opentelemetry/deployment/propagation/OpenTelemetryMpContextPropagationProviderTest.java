package io.quarkus.opentelemetry.deployment.propagation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;
import io.quarkus.opentelemetry.runtime.propagation.OpenTelemetryMpContextPropagationProvider;

/**
 * Plain unit test (no Quarkus boot) for {@link OpenTelemetryMpContextPropagationProvider}.
 * <p>
 * Focuses on the "cleared" context contract: when MicroProfile Context Propagation is configured to
 * clear the OpenTelemetry context (e.g. {@code ManagedExecutor} with {@code ThreadContext.ALL_REMAINING}
 * cleared), the task must run with no ambient OTel context, and the previous context must be restored
 * afterwards. Because managed threads are pooled, a context left on the target thread would otherwise
 * leak into a task that explicitly asked for a cleared context.
 * <p>
 * Runs off the Vert.x event loop, so {@link QuarkusContextStorage} exercises its ThreadLocal fallback
 * ({@code MDCEnabledContextStorage}) rather than the duplicated-context path.
 *
 * This has been implemented after the findings for ObservationMpContextPropagationProvider.
 */
public class OpenTelemetryMpContextPropagationProviderTest {

    private static final String LEAKED_TRACE_ID = "00000000000000000000000000000001";
    private static final String LEAKED_SPAN_ID = "0000000000000001";

    private static Context leakedContext() {
        SpanContext spanContext = SpanContext.create(LEAKED_TRACE_ID, LEAKED_SPAN_ID,
                TraceFlags.getSampled(), TraceState.getDefault());
        return Context.root().with(Span.wrap(spanContext));
    }

    @Test
    void clearedContextClearsAmbientContextAndRestoresItAfterwards() throws Exception {
        OpenTelemetryMpContextPropagationProvider provider = new OpenTelemetryMpContextPropagationProvider();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // Simulate a pooled managed thread that already holds an OTel context (left over from a
            // previous task). The context is attached directly on the worker thread and never closed.
            String precondition = executor.submit(() -> {
                @SuppressWarnings("unused")
                Scope leaked = QuarkusContextStorage.INSTANCE.attach(leakedContext());
                assertThat(Span.current().getSpanContext().isValid())
                        .as("precondition: the worker thread holds a leftover OTel context")
                        .isTrue();
                return Span.current().getSpanContext().getTraceId();
            }).get(5, TimeUnit.SECONDS);

            assertThat(precondition)
                    .as("precondition: the leftover context carries the seeded trace id")
                    .isEqualTo(LEAKED_TRACE_ID);

            // A new task submitted with the OpenTelemetry context CLEARED must not see the leftover context.
            ThreadContextSnapshot snapshot = provider.clearedContext(Map.of());
            boolean clearedInsideTask = executor.submit(() -> {
                ThreadContextController controller = snapshot.begin();
                try {
                    return Context.current() == Context.root();
                } finally {
                    controller.endContext();
                }
            }).get(5, TimeUnit.SECONDS);

            assertThat(clearedInsideTask)
                    .as("clearedContext must clear any ambient OTel context on the target thread")
                    .isTrue();

            // After the task ends, the previously present context must be restored on the worker thread.
            String restoredTraceId = executor.submit(
                    () -> Span.current().getSpanContext().getTraceId())
                    .get(5, TimeUnit.SECONDS);

            assertThat(restoredTraceId)
                    .as("clearedContext must restore the previous OTel context once the task completes")
                    .isEqualTo(LEAKED_TRACE_ID);
        } finally {
            executor.shutdownNow();
        }
    }
}
