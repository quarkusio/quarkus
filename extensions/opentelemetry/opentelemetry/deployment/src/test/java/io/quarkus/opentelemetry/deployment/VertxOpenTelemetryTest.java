package io.quarkus.opentelemetry.deployment;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_CLIENT_IP;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_FLAVOR;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_HOST;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_SCHEME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_STATUS_CODE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_TARGET;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_USER_AGENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.hamcrest.collection.ArrayMatching.arrayContainingInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class VertxOpenTelemetryTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(TestSpanExporter.class)
                    .addClass(TracerRouter.class)
                    .addClass(TestUtil.class));

    @Inject
    TestSpanExporter testSpanExporter;

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void trace() throws NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        RestAssured.when().get("/tracer").then()
                .statusCode(200)
                .body(is("Hello Tracer!"));

        List<SpanData> spans = testSpanExporter.getFinishedSpanItems();

        TextMapPropagator[] textMapPropagators = TestUtil.getTextMapPropagators(openTelemetry);
        IdGenerator idGenerator = TestUtil.getIdGenerator(openTelemetry);
        Sampler sampler = TestUtil.getSampler(openTelemetry);

        assertEquals(2, spans.size());
        assertEquals("io.quarkus.vertx.opentelemetry", spans.get(0).getName());
        assertEquals("hello!", spans.get(0).getAttributes().get(stringKey("test.message")));
        assertEquals(200, spans.get(1).getAttributes().get(HTTP_STATUS_CODE));
        assertEquals("1.1", spans.get(1).getAttributes().get(HTTP_FLAVOR));
        assertEquals("/tracer", spans.get(1).getAttributes().get(HTTP_TARGET));
        assertEquals("http", spans.get(1).getAttributes().get(HTTP_SCHEME));
        assertEquals("localhost:8081", spans.get(1).getAttributes().get(HTTP_HOST));
        assertEquals("127.0.0.1", spans.get(1).getAttributes().get(HTTP_CLIENT_IP));
        assertThat(textMapPropagators, arrayContainingInAnyOrder(W3CTraceContextPropagator.getInstance(),
                W3CBaggagePropagator.getInstance()));
        assertThat(idGenerator, instanceOf(IdGenerator.random().getClass()));
        assertThat(sampler.getDescription(), stringContainsInOrder("ParentBased", "AlwaysOnSampler"));
        assertNotNull(spans.get(1).getAttributes().get(HTTP_USER_AGENT));
    }
}
