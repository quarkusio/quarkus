package io.quarkus.opentelemetry.deployment;

import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.quarkus.opentelemetry.deployment.common.TestSpanExporter.getSpanByKindAndParentId;
import static io.quarkus.opentelemetry.deployment.common.TestUtil.assertStringAttribute;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.extension.annotations.WithSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.SemanticAttributes;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporterProvider;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenTelemetryHttpCDILegacyTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClass(TestUtil.class)
                            .addClass(HelloResource.class)
                            .addClass(HelloBean.class)
                            .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class)
                            .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider"))
            .withConfigurationResource("application-default.properties");

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

        SpanData server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertEquals("GET /hello", server.getName());
        assertEquals(SERVER, server.getKind());
        // verify that OpenTelemetryServerFilter took place
        assertStringAttribute(server, SemanticAttributes.CODE_NAMESPACE,
                "io.quarkus.opentelemetry.deployment.OpenTelemetryHttpCDILegacyTest$HelloResource");
        assertStringAttribute(server, SemanticAttributes.CODE_FUNCTION, "hello");

        SpanData internal = getSpanByKindAndParentId(spans, INTERNAL, server.getSpanId());
        assertEquals("HelloBean.hello", internal.getName());
        assertEquals(INTERNAL, internal.getKind());

        assertEquals(internal.getParentSpanId(), server.getSpanId());
    }

    @Test
    void withSpan() {
        RestAssured.when()
                .get("/hello/withSpan").then()
                .statusCode(200)
                .body(is("hello"));

        List<SpanData> spans = spanExporter.getFinishedSpanItems(3);

        final SpanData server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertEquals("GET /hello/withSpan", server.getName());

        final SpanData internalFromBean = getSpanByKindAndParentId(spans, INTERNAL, server.getSpanId());
        assertEquals("withSpan", internalFromBean.getName());

        final SpanData internalFromWithSpan = getSpanByKindAndParentId(spans, INTERNAL, internalFromBean.getSpanId());
        assertEquals("HelloBean.hello", internalFromWithSpan.getName());

        assertThat(internalFromBean.getTraceId()).isIn(internalFromWithSpan.getTraceId(), server.getTraceId());
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
