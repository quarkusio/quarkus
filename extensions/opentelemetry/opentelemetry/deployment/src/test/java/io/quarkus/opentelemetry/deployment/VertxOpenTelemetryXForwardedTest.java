package io.quarkus.opentelemetry.deployment;

import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_CLIENT_IP;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class VertxOpenTelemetryXForwardedTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(TestSpanExporter.class)
                    .addClass(TracerRouter.class));

    @Inject
    TestSpanExporter testSpanExporter;

    @Test
    void trace() {
        RestAssured.given().header("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178")
                .when().get("/tracer").then()
                .statusCode(200)
                .body(is("Hello Tracer!"));

        List<SpanData> spans = testSpanExporter.getFinishedSpanItems();

        assertEquals("203.0.113.195", spans.get(1).getAttributes().get(HTTP_CLIENT_IP));
    }
}
