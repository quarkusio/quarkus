package io.quarkus.opentelemetry.deployment.traces;

import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.incubating.CodeIncubatingAttributes.CODE_FUNCTION;
import static io.opentelemetry.semconv.incubating.CodeIncubatingAttributes.CODE_NAMESPACE;
import static io.quarkus.opentelemetry.deployment.common.TestUtil.assertStringAttribute;
import static io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter.getSpanByKindAndParentId;
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

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenTelemetryHttpCDITest {
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
            .withConfigurationResource("resource-config/application-no-metrics.properties");

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

        final SpanData server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertEquals("GET /hello", server.getName());
        // verify that OpenTelemetryServerFilter took place
        assertStringAttribute(server, CODE_NAMESPACE,
                "io.quarkus.opentelemetry.deployment.traces.OpenTelemetryHttpCDITest$HelloResource");
        assertStringAttribute(server, CODE_FUNCTION, "hello");

        final SpanData internalFromBean = getSpanByKindAndParentId(spans, INTERNAL, server.getSpanId());
        assertEquals("HelloBean.hello", internalFromBean.getName());

        assertEquals(internalFromBean.getTraceId(), server.getTraceId());
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

        final SpanData withSpan = getSpanByKindAndParentId(spans, INTERNAL, server.getSpanId());
        assertEquals("withSpan", withSpan.getName());

        final SpanData bean = getSpanByKindAndParentId(spans, INTERNAL, withSpan.getSpanId());
        assertEquals("HelloBean.hello", bean.getName());

        assertThat(bean.getTraceId()).isIn(withSpan.getTraceId(), server.getTraceId());
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
