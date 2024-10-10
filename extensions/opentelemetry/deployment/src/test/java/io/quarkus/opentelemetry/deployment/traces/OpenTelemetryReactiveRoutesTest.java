package io.quarkus.opentelemetry.deployment.traces;

import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_ROUTE;
import static io.quarkus.opentelemetry.deployment.common.TestUtil.assertStringAttribute;
import static io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter.getSpanByKindAndParentId;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Route;
import io.restassured.RestAssured;
import io.vertx.ext.web.RoutingContext;

public class OpenTelemetryReactiveRoutesTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(TestUtil.class)
                    .addClass(TestSpanExporter.class)
                    .addClass(TestSpanExporterProvider.class)
                    .addAsResource("resource-config/application-no-metrics.properties", "application.properties")
                    .addAsResource(
                            "META-INF/services-config/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider",
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider"));
    @Inject
    TestSpanExporter spanExporter;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
    }

    @Test
    void root() {
        RestAssured.when()
                .get("/").then()
                .statusCode(200)
                .body(is("hello"));

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);

        final SpanData server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertEquals("GET /", server.getName());
        assertStringAttribute(server, HTTP_ROUTE, "/");
    }

    @Test
    void nonRoot() {
        RestAssured.when()
                .get("/hello").then()
                .statusCode(200)
                .body(is("hello world"));

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);

        final SpanData server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertEquals("GET /hello", server.getName());
        assertStringAttribute(server, HTTP_ROUTE, "/hello");
    }

    @ApplicationScoped
    public static class Routes {

        @Route(path = "/", methods = Route.HttpMethod.GET)
        public void root(RoutingContext rc) {
            rc.response().end("hello");
        }

        @Route(path = "/hello", methods = Route.HttpMethod.GET)
        public void hello(RoutingContext rc) {
            String name = rc.request().getParam("name");
            if (name == null) {
                name = "world";
            }
            rc.response().end("hello " + name);
        }
    }
}
