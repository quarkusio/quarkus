package io.quarkus.opentelemetry.deployment.propagation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.quarkus.opentelemetry.runtime.propagation.OpenTelemetryMpContextPropagationProvider;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * Verifies that {@link OpenTelemetryMpContextPropagationProvider} does not leak
 * the originating thread's OTel context onto the target thread after
 * {@code endContext()} is called.
 *
 * <p>
 * The leak scenario: a plain Java thread (no Vert.x context, no prior OTel
 * context) receives a propagated span via {@code begin()}, then {@code endContext()}
 * should restore the thread to its original clean state. Without the fix, the
 * propagated span remains attached to the thread's storage.
 */
public class OpenTelemetryMpContextPropagationLeakTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(TestResource.class))
            .overrideConfigKey("quarkus.otel.metrics.enabled", "false")
            .overrideConfigKey("quarkus.otel.logs.enabled", "false")
            .overrideConfigKey("quarkus.otel.experimental.shutdown-wait-time", "1000ms")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false")
            .overrideConfigKey(
                    "quarkus.log.category.\"io.quarkus.opentelemetry.runtime.propagation.OpenTelemetryMpContextPropagationProvider\".level",
                    "DEBUG");

    @Test
    void contextNotLeakedAfterEndContext() {
        String response = RestAssured.when()
                .get("/test-leak").then()
                .statusCode(200)
                .extract().asString();

        String[] parts = response.split("\\|");
        String traceIdBefore = parts[0];
        boolean validAfterSubmit = Boolean.parseBoolean(parts[1]);
        String traceIdAfterBegin = parts[2];
        boolean validAfterEnd = Boolean.parseBoolean(parts[3]);
        String traceIdAfterEnd = parts[4];

        assertThat(validAfterSubmit)
                .as("Target thread should have no valid span before begin()")
                .isFalse();

        assertThat(traceIdAfterBegin)
                .as("Target thread should see the request's span during begin()")
                .isEqualTo(traceIdBefore);

        assertThat(validAfterEnd)
                .as("Target thread must not retain a valid span after endContext()")
                .isFalse();

        assertThat(traceIdAfterEnd)
                .as("Target thread must not retain the request's trace ID after endContext()")
                .isNotEqualTo(traceIdBefore);
    }

    @ApplicationScoped
    @Path("/")
    public static class TestResource {

        @GET
        @Path("/test-leak")
        public String testLeak() {
            SpanContext otelContextBefore = Span.current().getSpanContext();
            String traceIdBefore = otelContextBefore.getTraceId();

            OpenTelemetryMpContextPropagationProvider provider = new OpenTelemetryMpContextPropagationProvider();
            ThreadContextSnapshot snapshot = provider.currentContext(Map.of());

            try (ExecutorService exec = Executors.newSingleThreadExecutor();) {
                Future<String> result = exec.submit(() -> {
                    boolean validAfterSubmit = Span.current().getSpanContext().isValid();

                    ThreadContextController controller = snapshot.begin();
                    String traceIdAfterBegin = Span.current().getSpanContext().getTraceId();

                    controller.endContext();
                    SpanContext spanAfterEnd = Span.current().getSpanContext();

                    return validAfterSubmit + "|" + traceIdAfterBegin
                            + "|" + spanAfterEnd.isValid() + "|" + spanAfterEnd.getTraceId();
                });

                return traceIdBefore + "|" + result.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
