package io.quarkus.opentelemetry.deployment;

import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.config.SmallRyeConfig;

public class OpenTelemetryResourceTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(TestSpanExporter.class)
                    .addAsResource("resource-config/application.properties", "application.properties"));

    @Inject
    SmallRyeConfig config;
    @Inject
    TestSpanExporter spanExporter;

    @Test
    void resource() {
        RestAssured.when()
                .get("/hello").then()
                .statusCode(200)
                .body(is("hello"));

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
        assertEquals("/hello", spans.get(0).getName());
        assertEquals(SERVER, spans.get(0).getKind());
        assertEquals("authservice", spans.get(0).getResource().getAttribute(AttributeKey.stringKey("service.name")));
        assertEquals(config.getRawValue("quarkus.uuid"),
                spans.get(0).getResource().getAttribute(AttributeKey.stringKey("service.instance.id")));
    }

    @Path("/hello")
    public static class HelloResource {
        @GET
        public String hello() {
            return "hello";
        }
    }
}
