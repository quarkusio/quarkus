package io.quarkus.opentelemetry.deployment;

import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.extension.annotations.WithSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenTelemetryHttpCDITest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClass(HelloResource.class)
                            .addClass(HelloBean.class)
                            .addClass(TestSpanExporter.class));

    @Inject
    TestSpanExporter spanExporter;

    @Test
    void telemetry() {
        RestAssured.when()
                .get("/hello").then()
                .statusCode(200)
                .body(is("hello"));

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);
        assertEquals("HelloBean.hello", spans.get(0).getName());
        assertEquals(INTERNAL, spans.get(0).getKind());
        assertEquals("/hello", spans.get(1).getName());
        assertEquals(SERVER, spans.get(1).getKind());
        assertEquals(spans.get(0).getParentSpanId(), spans.get(1).getSpanId());
    }

    @Path("/hello")
    public static class HelloResource {
        @Inject
        HelloBean helloBean;

        @GET
        public String hello() {
            return helloBean.hello();
        }
    }

    @ApplicationScoped
    public static class HelloBean {
        @WithSpan
        public String hello() {
            return "hello";
        }
    }
}
