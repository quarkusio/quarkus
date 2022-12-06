package io.quarkus.opentelemetry.deployment;

import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporter;
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

    @AfterEach
    void tearDown() {
        spanExporter.reset();
    }

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

    @Test
    void withSpan() {
        RestAssured.when()
                .get("/hello/withSpan").then()
                .statusCode(200)
                .body(is("hello"));

        List<SpanData> spans = spanExporter.getFinishedSpanItems(3);
        assertEquals("HelloBean.hello", spans.get(0).getName());
        assertEquals(INTERNAL, spans.get(0).getKind());
        assertEquals("withSpan", spans.get(1).getName());
        assertEquals(INTERNAL, spans.get(1).getKind());
        assertEquals("/hello/withSpan", spans.get(2).getName());
        assertEquals(SERVER, spans.get(2).getKind());
        assertEquals(spans.get(0).getParentSpanId(), spans.get(1).getSpanId());
        assertEquals(spans.get(1).getParentSpanId(), spans.get(2).getSpanId());
        assertThat(spans.get(0).getTraceId()).isIn(spans.get(1).getTraceId(), spans.get(2).getTraceId());
    }

    @Path("/hello")
    public static class HelloResource {
        @Inject
        HelloBean helloBean;

        @GET
        public String hello() {
            return helloBean.hello();
        }

        @GET
        @Path("/withSpan")
        @WithSpan("withSpan")
        public String withSpan() {
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
