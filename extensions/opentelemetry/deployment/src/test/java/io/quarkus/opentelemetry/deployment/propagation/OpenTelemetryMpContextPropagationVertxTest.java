package io.quarkus.opentelemetry.deployment.propagation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;
import io.quarkus.opentelemetry.runtime.propagation.OpenTelemetryMpContextPropagationProvider;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * Complext test scenario verifying that {@link OpenTelemetryMpContextPropagationProvider} does not
 * trash the Vert.x duplicated context (DC) when OTel scopes created OUTSIDE the
 * begin/endContext window close INSIDE it.
 *
 * <p>
 * The trashing scenario: on a Vert.x event loop thread, begin() attaches a
 * captured context and records the DC's prior state ({@code otelBeforeAttach}).
 * Between the ThreadContextProvider begin() and endContext(), other OTel scopes close (spans end),
 * leaving the DC in a clean state. Without a guard, endContext()'s
 * scope.close() unconditionally restores the stale {@code otelBeforeAttach},
 * putting a dead span's context back on the DC.
 */
public class OpenTelemetryMpContextPropagationVertxTest {

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
    void contextNotTrashedWhenExternalScopesCloseInsideBeginEnd() {
        String response = RestAssured.when()
                .get("/test-vertx-scope-guard").then()
                .statusCode(200)
                .extract().asString();

        String[] parts = response.split("\\|");
        String requestSpanId = parts[0];
        String afterBeginSpanId = parts[1];
        String afterEndSpanId = parts[2];
        String grandchildSpanId = parts[3];

        assertThat(afterBeginSpanId)
                .as("After begin(), the captured child context should be current")
                .isNotEqualTo(requestSpanId);

        assertThat(afterEndSpanId)
                .as("After endContext(), the DC should have the request span, not the stale grandchild")
                .isEqualTo(requestSpanId);

        assertThat(afterEndSpanId)
                .as("After endContext(), the DC must not hold the stale grandchild context")
                .isNotEqualTo(grandchildSpanId);
    }

    @ApplicationScoped
    @Path("/")
    public static class TestResource {

        @Inject
        Tracer tracer;

        @GET
        @Path("/test-vertx-scope-guard")
        public String testVertxScopeGuard() {
            // Running on Vert.x event loop with duplicated context (DC).
            // DC has the HTTP server's request span.
            Context requestCtx = QuarkusContextStorage.INSTANCE.current();
            String requestSpanId = Span.fromContext(requestCtx).getSpanContext().getSpanId();

            // Create a nested span hierarchy: request → child → grandchild
            Span childSpan = tracer.spanBuilder("child").startSpan();
            Context childCtx = childSpan.storeInContext(requestCtx);
            Scope childOtelScope = QuarkusContextStorage.INSTANCE.attach(childCtx);
            // DC = childCtx, childOtelScope.otelBeforeAttach = requestCtx

            Span grandchildSpan = tracer.spanBuilder("grandchild").startSpan();
            Context grandchildCtx = grandchildSpan.storeInContext(childCtx);
            Scope grandchildOtelScope = QuarkusContextStorage.INSTANCE.attach(grandchildCtx);
            String grandchildSpanId = grandchildSpan.getSpanContext().getSpanId();
            // DC = grandchildCtx, grandchildOtelScope.otelBeforeAttach = childCtx

            // Temporarily restore childCtx to capture a snapshot at that level
            grandchildOtelScope.close(); // DC = childCtx

            OpenTelemetryMpContextPropagationProvider provider = new OpenTelemetryMpContextPropagationProvider();
            ThreadContextSnapshot snapshot = provider.currentContext(Map.of());
            // capturedCtx = childCtx

            // Re-attach grandchild so begin() will see a different context
            grandchildOtelScope = QuarkusContextStorage.INSTANCE.attach(grandchildCtx);
            // DC = grandchildCtx

            // begin() — DC has grandchildCtx, snapshot has childCtx
            // childCtx != grandchildCtx → QuarkusOTelScope(DC, childCtx, otelBeforeAttach=grandchildCtx)
            // DC = childCtx
            ThreadContextController controller = snapshot.begin();

            String afterBeginSpanId = Span.current().getSpanContext().getSpanId();

            // --- Between begin() and endContext(), external OTel scopes close ---
            // This simulates spans ending during async processing (e.g. GraphQL
            // operation and resolver spans finishing while MP context propagation
            // is still active).

            grandchildOtelScope.close();
            // DC restored to childCtx (grandchild's otelBeforeAttach)
            grandchildSpan.end();

            childOtelScope.close();
            // DC restored to requestCtx (child's otelBeforeAttach)
            childSpan.end();

            // DC is now requestCtx — correct clean state after all child spans ended.
            // But scope_MP has otelBeforeAttach = grandchildCtx (STALE!)

            // endContext()
            controller.endContext();
            // Without guard: unconditionally restores grandchildCtx → TRASHED
            // With guard: current(requestCtx) != capturedCtx(childCtx) → skip → requestCtx (ok)

            SpanContext afterEndCtx = Span.current().getSpanContext();
            String afterEndSpanId = afterEndCtx.getSpanId();

            return requestSpanId + "|" + afterBeginSpanId + "|" + afterEndSpanId + "|" + grandchildSpanId;
        }
    }
}
