package io.quarkus.opentelemetry.deployment.metrics;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.UrlAttributes.URL_SCHEME;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.hamcrest.Matchers.is;

import java.net.URL;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.metrics.data.MetricData;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporterProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;

public class MpHttpServerMetricsTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(InMemoryMetricExporter.class, InMemoryMetricExporterProvider.class)
                            .addAsResource(new StringAsset(InMemoryMetricExporterProvider.class.getCanonicalName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider")
                            .add(new StringAsset(
                                    "quarkus.otel.metrics.enabled=true\n" +
                                            "quarkus.otel.traces.exporter=none\n" +
                                            "quarkus.otel.logs.exporter=none\n" +
                                            "quarkus.otel.metrics.exporter=in-memory\n" +
                                            "quarkus.otel.metric.export.interval=300ms\n"),
                                    "application.properties"));

    @Inject
    protected InMemoryMetricExporter metricExporter;

    @TestHTTPResource
    URL url;

    @AfterEach
    void tearDown() {
        metricExporter.reset();
    }

    @Test
    void collectsHttpRouteFromEndAttributes() {
        RestAssured.when()
                .get("/span").then()
                .statusCode(200)
                .body(is("hello"));

        RestAssured.when()
                .get("/fail").then()
                .statusCode(INTERNAL_SERVER_ERROR.getStatusCode());

        metricExporter.assertCountPointsAtLeast("http.server.request.duration", null, 2);
        MetricData metric = metricExporter
                .getFinishedMetricItems("http.server.request.duration", null).stream()
                .reduce((first, second) -> second) // get the last received
                .orElse(null);

        assertThat(metric)
                .hasName("http.server.request.duration")
                .hasDescription("Duration of HTTP server requests.")
                .hasUnit("s")
                .hasHistogramSatisfying(histogram -> histogram.isCumulative()
                        .hasPointsSatisfying(
                                point -> point.hasCount(1)
                                        .hasAttributesSatisfying(
                                                equalTo(HTTP_REQUEST_METHOD, "GET"),
                                                equalTo(URL_SCHEME, "http"),
                                                equalTo(HTTP_RESPONSE_STATUS_CODE, 200),
                                                equalTo(HTTP_ROUTE, url.getPath() + "span")),
                                point -> point.hasCount(1)
                                        .hasAttributesSatisfying(
                                                equalTo(HTTP_REQUEST_METHOD, "GET"),
                                                equalTo(URL_SCHEME, "http"),
                                                equalTo(HTTP_RESPONSE_STATUS_CODE, 500),
                                                equalTo(HTTP_ROUTE, url.getPath() + "fail"))));
    }

    @Path("/")
    public static class SpanResource {
        @GET
        @Path("/span")
        public Response span() {
            return Response.ok("hello").build();
        }

        @GET
        @Path("/fail")
        public Response fail() {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }
}
