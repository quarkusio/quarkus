package io.quarkus.opentelemetry.deployment.instrumentation;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_CLIENT_IP;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_SCHEME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_STATUS_CODE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_TARGET;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_NAME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_PORT;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.USER_AGENT_ORIGINAL;
import static io.quarkus.opentelemetry.deployment.common.TestSpanExporter.getSpanByKindAndParentId;
import static io.restassured.RestAssured.given;
import static io.vertx.core.http.HttpMethod.GET;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.hamcrest.collection.ArrayMatching.arrayContainingInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporterProvider;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.opentelemetry.deployment.common.TracerRouter;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class VertxOpenTelemetryTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(TracerRouter.class)
                    .addClass(TestUtil.class)
                    .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class)
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider"))
            //            .overrideConfigKey(SmallRyeConfig.SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN, "false")// FIXME default config mapping
            .overrideConfigKey("quarkus.otel.traces.exporter", "test-span-exporter")
            .overrideConfigKey("quarkus.otel.metrics.exporter", "none")
            .overrideConfigKey("quarkus.otel.logs.exporter", "none")
            .overrideConfigKey("quarkus.otel.bsp.schedule.delay", "200");

    @Inject
    TestSpanExporter spanExporter;
    @Inject
    OpenTelemetry openTelemetry;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
    }

    @Test
    void trace() throws NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        RestAssured.when().get("/tracer").then()
                .statusCode(200)
                .body(is("Hello Tracer!"));

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);

        TextMapPropagator[] textMapPropagators = TestUtil.getTextMapPropagators(openTelemetry);
        IdGenerator idGenerator = TestUtil.getIdGenerator(openTelemetry);
        Sampler sampler = TestUtil.getSampler(openTelemetry);

        SpanData server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertEquals(HTTP_OK, server.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals("/tracer", server.getAttributes().get(HTTP_TARGET));
        assertEquals("http", server.getAttributes().get(HTTP_SCHEME));
        assertEquals("localhost", server.getAttributes().get(NET_HOST_NAME));
        assertEquals("8081", server.getAttributes().get(NET_HOST_PORT).toString());
        assertEquals("127.0.0.1", server.getAttributes().get(HTTP_CLIENT_IP));
        assertThat(textMapPropagators, arrayContainingInAnyOrder(W3CTraceContextPropagator.getInstance(),
                W3CBaggagePropagator.getInstance()));
        assertThat(idGenerator, instanceOf(IdGenerator.random().getClass()));
        assertThat(sampler.getDescription(), stringContainsInOrder("ParentBased", "AlwaysOnSampler"));
        assertNotNull(server.getAttributes().get(USER_AGENT_ORIGINAL));

        SpanData internal = getSpanByKindAndParentId(spans, INTERNAL, server.getSpanId());
        assertEquals("io.quarkus.vertx.opentelemetry", internal.getName());
        assertEquals("hello!", internal.getAttributes().get(stringKey("test.message")));
    }

    @Test
    void spanNameWithoutQueryString() {
        RestAssured.when().get("/tracer?id=1").then()
                .statusCode(200)
                .body(is("Hello Tracer!"));

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);

        final SpanData server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertEquals("GET /tracer", server.getName());
        assertEquals(HTTP_OK, server.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals("/tracer?id=1", server.getAttributes().get(HTTP_TARGET));
        assertEquals("http", server.getAttributes().get(HTTP_SCHEME));
        assertEquals("localhost", server.getAttributes().get(NET_HOST_NAME));
        assertEquals("8081", server.getAttributes().get(NET_HOST_PORT).toString());
        assertEquals("127.0.0.1", server.getAttributes().get(HTTP_CLIENT_IP));
        assertNotNull(server.getAttributes().get(USER_AGENT_ORIGINAL));

        SpanData internal = getSpanByKindAndParentId(spans, INTERNAL, server.getSpanId());
        assertEquals("io.quarkus.vertx.opentelemetry", internal.getName());
        assertEquals("hello!", internal.getAttributes().get(stringKey("test.message")));

    }

    @Test
    void spanPath() {
        given()
                .get("/hello/{name}", "Naruto")
                .then()
                .statusCode(HTTP_OK)
                .body(equalTo("hello Naruto"));

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
        assertEquals(1, spans.size());

        assertEquals("GET /hello/:name", spans.get(0).getName());
        assertEquals(HTTP_OK, spans.get(0).getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(GET.toString(), spans.get(0).getAttributes().get(HTTP_METHOD));
        assertEquals("/hello/:name", spans.get(0).getAttributes().get(HTTP_ROUTE));
    }

    @Test
    void notFound() {
        RestAssured.when().get("/notFound").then().statusCode(404);

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
        assertEquals(1, spans.size());

        assertEquals("GET /*", spans.get(0).getName());
        assertEquals("/*", spans.get(0).getAttributes().get(HTTP_ROUTE));
        assertEquals(HTTP_NOT_FOUND, spans.get(0).getAttributes().get(HTTP_STATUS_CODE));
    }

    @Test
    void notFoundPath() {
        given()
                .get("/hello/{name}", "Goku")
                .then()
                .statusCode(HTTP_NOT_FOUND);

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
        assertEquals(1, spans.size());

        assertEquals("GET /hello/:name", spans.get(0).getName());
        assertEquals(HTTP_NOT_FOUND, spans.get(0).getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(GET.toString(), spans.get(0).getAttributes().get(HTTP_METHOD));
        assertEquals("/hello/:name", spans.get(0).getAttributes().get(HTTP_ROUTE));
    }
}
