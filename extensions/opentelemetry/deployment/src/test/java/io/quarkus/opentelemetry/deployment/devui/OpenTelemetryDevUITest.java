package io.quarkus.opentelemetry.deployment.devui;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import tools.jackson.databind.JsonNode;

public class OpenTelemetryDevUITest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((JavaArchive jar) -> jar
                    .addClasses(NestedResource.class, BoomResource.class, ReactiveResource.class,
                            MultiResource.class)
                    .addAsResource(new StringAsset(
                            // Exporter none: spans are still generated and delivered to our
                            // custom dev SpanProcessor; only OTLP export is disabled.
                            "quarkus.otel.traces.exporter=none\n"
                                    // Also disable metric/log OTLP export so nothing tries to
                                    // reach a (non-existent) collector in this hermetic test.
                                    + "quarkus.otel.metrics.exporter=none\n"
                                    + "quarkus.otel.logs.exporter=none\n"
                                    // Keep the test hermetic: no LGTM/observability dev service
                                    // container (which would also override the OTLP exporter).
                                    + "quarkus.devservices.enabled=false\n"),
                            "application.properties"));

    public OpenTelemetryDevUITest() {
        super("quarkus-opentelemetry");
    }

    @Test
    public void capturesAndGroupsParentChildSpans() throws Exception {
        RestAssured.when().get("/nested").then().statusCode(200);

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            JsonNode snapshot = super.executeJsonRPCMethod("getSnapshot");
            JsonNode traces = snapshot.get("traces");
            assertThat(traces).isNotNull();
            assertThat(traces.isArray()).isTrue();
            assertThat(traces.size()).isPositive();

            boolean hasParentChild = false;
            for (JsonNode trace : traces) {
                JsonNode spans = trace.get("spans");
                if (spans == null || spans.size() < 2) {
                    continue;
                }
                Set<String> spanIds = new HashSet<>();
                spans.forEach(s -> spanIds.add(s.get("spanId").asText()));
                for (JsonNode s : spans) {
                    String parent = s.get("parentSpanId").asText("");
                    if (!parent.isEmpty() && spanIds.contains(parent)) {
                        hasParentChild = true;
                    }
                }
            }
            assertThat(hasParentChild)
                    .as("a trace should group a parent span and its child via parentSpanId")
                    .isTrue();
        });
    }

    @Test
    public void capturesErrorStatus() throws Exception {
        RestAssured.when().get("/boom").then().statusCode(500);
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            JsonNode snapshot = super.executeJsonRPCMethod("getSnapshot");
            // Assert the actual captured status field, not just that "ERROR" appears somewhere
            // in the serialized snapshot: the failing request must yield a span whose
            // statusCode is ERROR.
            JsonNode errorSpan = firstSpanMatching(snapshot,
                    s -> "ERROR".equals(s.get("statusCode").asText()));
            assertThat(errorSpan)
                    .as("the failing /boom request should produce a span with ERROR status")
                    .isNotNull();
        });
    }

    @Test
    public void capturesReactiveSpanOffRequestThread() throws Exception {
        RestAssured.when().get("/reactive").then().statusCode(200);
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            JsonNode snapshot = super.executeJsonRPCMethod("getSnapshot");
            // A Uni endpoint completes off the request thread; assert on the span name field
            // rather than a substring anywhere in the snapshot.
            JsonNode span = firstSpanMatching(snapshot,
                    s -> s.get("name").asText().contains("/reactive"));
            assertThat(span)
                    .as("the reactive Uni endpoint should be captured as a span")
                    .isNotNull();
        });
    }

    @Test
    public void capturesMultiReactiveSpan() throws Exception {
        RestAssured.when().get("/multi").then().statusCode(200);
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            JsonNode snapshot = super.executeJsonRPCMethod("getSnapshot");
            // A Multi (SSE) endpoint streams items and completes off the request thread;
            // its server span must still be captured.
            JsonNode span = firstSpanMatching(snapshot,
                    s -> s.get("name").asText().contains("/multi"));
            assertThat(span)
                    .as("the reactive Multi (SSE) endpoint should be captured as a span")
                    .isNotNull();
        });
    }

    /** Returns the first span (across all traces) matching the predicate, or {@code null}. */
    private static JsonNode firstSpanMatching(JsonNode snapshot, Predicate<JsonNode> predicate) {
        JsonNode traces = snapshot.get("traces");
        if (traces == null) {
            return null;
        }
        List<JsonNode> spans = new ArrayList<>();
        for (JsonNode trace : traces) {
            JsonNode traceSpans = trace.get("spans");
            if (traceSpans != null) {
                traceSpans.forEach(spans::add);
            }
        }
        return spans.stream().filter(predicate).findFirst().orElse(null);
    }

    @Path("/nested")
    public static class NestedResource {
        @Inject
        Tracer tracer;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String nested() {
            Span child = tracer.spanBuilder("child-work").startSpan();
            try (Scope scope = child.makeCurrent()) {
                return "ok";
            } finally {
                child.end();
            }
        }
    }

    @Path("/boom")
    public static class BoomResource {
        @GET
        public String boom() {
            throw new RuntimeException("boom");
        }
    }

    @Path("/reactive")
    public static class ReactiveResource {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public Uni<String> reactive() {
            return Uni.createFrom().item("ok")
                    .onItem().delayIt().by(Duration.ofMillis(10));
        }
    }

    @Path("/multi")
    public static class MultiResource {
        @GET
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public Multi<String> multi() {
            return Multi.createFrom().items("a", "b", "c");
        }
    }
}
