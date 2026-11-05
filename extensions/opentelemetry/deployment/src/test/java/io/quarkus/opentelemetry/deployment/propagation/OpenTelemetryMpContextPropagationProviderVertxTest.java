package io.quarkus.opentelemetry.deployment.propagation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;
import io.quarkus.opentelemetry.runtime.propagation.OpenTelemetryMpContextPropagationProvider;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * Vert.x counterpart of {@link OpenTelemetryMpContextPropagationProviderTest}: verifies the
 * {@code clearedContext} contract on a Vert.x duplicated context (DC), where {@link QuarkusContextStorage}
 * uses the DC-backed storage path rather than the {@code MDCEnabledContextStorage} ThreadLocal
 * fallback that the plain unit test exercises.
 *
 * <p>
 * The resource is a blocking JAX-RS method, so it is dispatched on a worker thread bound to a Vert.x
 * duplicated context (not the event loop itself); either way {@link QuarkusContextStorage} resolves to
 * the DC-backed path. The DC already carries the HTTP request span (the "ambient" context that would
 * otherwise leak into a task that explicitly asked for a cleared OTel context). When MicroProfile
 * Context Propagation is configured to clear the OpenTelemetry context, the task must run with an
 * empty (root) OTel context, and the previous context must be restored on the DC once the task
 * completes.
 */
public class OpenTelemetryMpContextPropagationProviderVertxTest {
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
                    "DEBUG")
            .overrideConfigKey(
                    "quarkus.log.category.\"io.quarkus.opentelemetry.runtime.QuarkusContextStorage\".level",
                    "DEBUG");

    @Test
    void clearedContextClearsAmbientDuplicatedContextAndRestoresItAfterwards() {
        String response = RestAssured.when()
                .get("/test-vertx-cleared-context").then()
                .statusCode(200)
                .extract().asString();

        String[] parts = response.split("\\|");
        String requestSpanId = parts[0];
        boolean requestSpanValid = Boolean.parseBoolean(parts[1]);
        boolean taskSpanCleared = Boolean.parseBoolean(parts[2]);
        boolean taskSpanIsValid = Boolean.parseBoolean(parts[3]);
        boolean storageHasRootBeforeEnd = Boolean.parseBoolean(parts[4]);
        String restoredSpanId = parts[5];

        assertThat(requestSpanValid)
                .as("precondition: the Vert.x duplicated context holds the request's OTel context")
                .isTrue();

        assertThat(taskSpanCleared)
                .as("clearedContext must clear any ambient OTel context on the duplicated context")
                .isTrue();

        assertThat(taskSpanIsValid)
                .as("no valid span must be visible while the OTel context is cleared")
                .isFalse();

        assertThat(storageHasRootBeforeEnd)
                .as("the storage must hold the root context right before endContext()")
                .isTrue();

        assertThat(restoredSpanId)
                .as("clearedContext must restore the previous OTel context on the DC once the task completes")
                .isEqualTo(requestSpanId);
    }

    @Path("/")
    public static class TestResource {

        @GET
        @Path("/test-vertx-cleared-context")
        public String testVertxClearedContext() {
            // Running on a Vert.x duplicated context (DC) that already carries the HTTP server's
            // request span. This is the ambient context that clearedContext must hide.
            Context requestCtx = QuarkusContextStorage.INSTANCE.current();
            String requestSpanId = Span.fromContext(requestCtx).getSpanContext().getSpanId();
            boolean requestSpanValid = Span.fromContext(requestCtx).getSpanContext().isValid();

            OpenTelemetryMpContextPropagationProvider provider = new OpenTelemetryMpContextPropagationProvider();
            ThreadContextSnapshot snapshot = provider.clearedContext(Map.of());

            // begin() attaches Context.root() to the DC, hiding the request span.
            ThreadContextController controller = snapshot.begin();
            boolean taskSpanCleared = Context.current() == Context.root();
            boolean taskSpanIsValid = Span.current().getSpanContext().isValid();
            boolean storageHasRootBeforeEnd = QuarkusContextStorage.INSTANCE.current() == Context.root();

            // endContext() must restore the request span onto the DC.
            controller.endContext();
            String restoredSpanId = Span.fromContext(QuarkusContextStorage.INSTANCE.current())
                    .getSpanContext().getSpanId();

            return requestSpanId + "|" + requestSpanValid + "|" + taskSpanCleared
                    + "|" + taskSpanIsValid + "|" + storageHasRootBeforeEnd + "|" + restoredSpanId;
        }
    }
}
