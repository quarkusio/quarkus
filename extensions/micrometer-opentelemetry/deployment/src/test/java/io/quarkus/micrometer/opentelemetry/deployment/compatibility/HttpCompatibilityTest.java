package io.quarkus.micrometer.opentelemetry.deployment.compatibility;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.restassured.RestAssured.when;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Comparator;
import java.util.List;

import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.quarkus.micrometer.opentelemetry.deployment.common.HelloResource;
import io.quarkus.micrometer.opentelemetry.deployment.common.InMemoryMetricExporter;
import io.quarkus.micrometer.opentelemetry.deployment.common.InMemoryMetricExporterProvider;
import io.quarkus.micrometer.opentelemetry.deployment.common.PingPongResource;
import io.quarkus.micrometer.opentelemetry.deployment.common.ServletEndpoint;
import io.quarkus.micrometer.opentelemetry.deployment.common.Util;
import io.quarkus.micrometer.opentelemetry.deployment.common.VertxWebEndpoint;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Copy of io.quarkus.micrometer.deployment.binder.UriTagTest
 */
public class HttpCompatibilityTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Util.class,
                                    PingPongResource.class,
                                    PingPongResource.PingPongRestClient.class,
                                    ServletEndpoint.class,
                                    VertxWebEndpoint.class,
                                    HelloResource.class)
                            .addClasses(InMemoryMetricExporter.class, InMemoryMetricExporterProvider.class)
                            .addAsResource(new StringAsset(InMemoryMetricExporterProvider.class.getCanonicalName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider")
                            .add(new StringAsset("""
                                    quarkus.otel.metrics.exporter=in-memory\n
                                    quarkus.otel.metric.export.interval=100ms\n
                                    quarkus.micrometer.binder-enabled-default=false\n
                                    quarkus.micrometer.binder.http-client.enabled=true\n
                                    quarkus.micrometer.binder.http-server.enabled=true\n
                                    quarkus.micrometer.binder.http-server.match-patterns=/one=/two\n
                                    quarkus.micrometer.binder.http-server.ignore-patterns=/two\n
                                    quarkus.micrometer.binder.vertx.enabled=true\n
                                    pingpong/mp-rest/url=${test.url}\n
                                    quarkus.redis.devservices.enabled=false\n
                                    """),
                                    "application.properties"));
    public static final AttributeKey<String> URI = AttributeKey.stringKey("uri");
    public static final AttributeKey<String> METHOD = AttributeKey.stringKey("method");
    public static final AttributeKey<String> STATUS = AttributeKey.stringKey("status");

    @Inject
    protected InMemoryMetricExporter metricExporter;

    @BeforeEach
    void setUp() {
        metricExporter.reset();
    }

    /**
     * Same as io.quarkus.micrometer.deployment.binder.UriTagTest.
     * Makes sure we are getting equivalent results in OTel.
     * Micrometer uses timers and OTel uses histograms.
     */
    @Test
    void testHttpTimerToHistogramCompatibility() {
        RestAssured.basePath = "/";

        // Server GET vs. HEAD methods -- templated
        when().get("/hello/one").then().statusCode(200);
        when().get("/hello/two").then().statusCode(200);
        when().head("/hello/three").then().statusCode(200);
        when().head("/hello/four").then().statusCode(200);
        when().get("/vertx/echo/thing1").then().statusCode(200);
        when().get("/vertx/echo/thing2").then().statusCode(200);
        when().head("/vertx/echo/thing3").then().statusCode(200);
        when().head("/vertx/echo/thing4").then().statusCode(200);

        // Server -> Rest client -> Server (templated)
        when().get("/ping/one").then().statusCode(200);
        when().get("/ping/two").then().statusCode(200);
        when().get("/ping/three").then().statusCode(200);
        when().get("/ping/400").then().statusCode(200);
        when().get("/ping/500").then().statusCode(200);
        when().get("/async-ping/one").then().statusCode(200);
        when().get("/async-ping/two").then().statusCode(200);
        when().get("/async-ping/three").then().statusCode(200);

        // Server paths (templated)
        when().get("/one").then().statusCode(200);
        when().get("/two").then().statusCode(200);
        when().get("/vertx/item/123").then().statusCode(200);
        when().get("/vertx/item/1/123").then().statusCode(200);
        //        when().get("/servlet/12345").then().statusCode(200);

        Awaitility.await().atMost(10, SECONDS)
                .untilAsserted(() -> {
                    final List<MetricData> metricDataList = metricExporter.getFinishedMetricItems("http.server.requests", null);
                    final MetricData metricData = metricDataList.get(metricDataList.size() - 1); // get last collected
                    assertServerMetrics(metricData);
                });

        final List<MetricData> metricDataList = metricExporter.getFinishedMetricItems("http.server.requests", null);
        final MetricData metricData = metricDataList.stream()
                .max(Comparator.comparingInt(data -> data.getData().getPoints().size()))
                .get();

        assertThat(metricData.getInstrumentationScopeInfo().getName())
                .isEqualTo("io.opentelemetry.micrometer-1.5");

        // /one should map to /two, which is ignored.
        // Neither should exist w/ timers because they were disabled in the configuration.
        assertThat(metricData.getHistogramData().getPoints().stream()
                .anyMatch(point -> point.getAttributes().get(URI).equals("/one") ||
                        point.getAttributes().get(URI).equals("/two")))
                .isFalse();

        // OTel metrics are not enabled
        assertThat(metricExporter.getFinishedMetricItem("http.server.request.duration")).isNull();

        metricExporter.assertCountDataPointsAtLeast("http.client.requests", null, 2);
        final List<MetricData> clientMetricDataList = metricExporter.getFinishedMetricItems("http.client.requests", null);

        Awaitility.await().atMost(10, SECONDS)
                .untilAsserted(() -> {
                    final MetricData clientMetricData = clientMetricDataList.get(clientMetricDataList.size() - 1); // get last collected
                    assertThat(clientMetricData.getInstrumentationScopeInfo().getName())
                            .isEqualTo("io.opentelemetry.micrometer-1.5");
                    assertThat(clientMetricData)
                            .hasName("http.client.requests") // in OTel it should be "http.server.request.duration"
                            .hasDescription("") // in OTel it should be "Duration of HTTP client requests."
                            .hasUnit("ms") // OTel has seconds
                            .hasHistogramSatisfying(histogram -> histogram.isCumulative()
                                    .hasPointsSatisfying(
                                            // valid entries
                                            point -> point.hasCount(1)
                                                    .hasAttributesSatisfying(
                                                            // "uri" not following conventions and should be "http.route"
                                                            equalTo(URI, "/pong/{message}"),
                                                            // Method not following conventions and should be "http.request.method"
                                                            equalTo(METHOD, "GET"),
                                                            // status_code not following conventions and should be
                                                            // "http.response.status_code" and it should use a long key and not a string key
                                                            equalTo(STATUS, "400")),
                                            point -> point.hasCount(1)
                                                    .hasAttributesSatisfying(
                                                            equalTo(URI, "/pong/{message}"),
                                                            equalTo(METHOD, "GET"),
                                                            equalTo(STATUS, "500")),
                                            point -> point.hasCount(6) // 3 sync requests and 3 async requests
                                                    .hasAttributesSatisfying(
                                                            equalTo(URI, "/pong/{message}"),
                                                            equalTo(METHOD, "GET"),
                                                            equalTo(STATUS, "200"))));
                });
    }

    private static void assertServerMetrics(MetricData metricData) {
        assertThat(metricData)
                .hasName("http.server.requests") // in OTel it should be "http.server.request.duration"
                .hasDescription("HTTP server request processing time") // in OTel it should be "Duration of HTTP server requests."
                .hasUnit("ms") // OTel has seconds
                .hasHistogramSatisfying(histogram -> histogram.isCumulative()
                        .hasPointsSatisfying(
                                // valid entries
                                point -> point.hasCount(1)
                                        .hasAttributesSatisfying(
                                                // "uri" not following conventions and should be "http.route"
                                                equalTo(URI, "/vertx/item/{id}"),
                                                // method not following conventions and should be "http.request.method"
                                                equalTo(METHOD, "GET"),
                                                // status_code not following conventions and should be
                                                // "http.response.status_code" and it should use a long key and not a string key
                                                equalTo(STATUS, "200")),
                                point -> point.hasCount(1)
                                        .hasAttributesSatisfying(
                                                equalTo(URI, "/vertx/item/{id}/{sub}"),
                                                equalTo(METHOD, "GET"),
                                                equalTo(STATUS, "200")),
                                point -> point.hasCount(2)
                                        .hasAttributesSatisfying(
                                                equalTo(URI, "/hello/{message}"),
                                                equalTo(METHOD, "HEAD"),
                                                equalTo(STATUS, "200")),
                                point -> point.hasCount(2)
                                        .hasAttributesSatisfying(
                                                equalTo(URI, "/hello/{message}"),
                                                equalTo(METHOD, "GET"),
                                                equalTo(STATUS, "200")),
                                point -> point.hasCount(2)
                                        .hasAttributesSatisfying(
                                                equalTo(URI, "/vertx/echo/{msg}"),
                                                equalTo(METHOD, "HEAD"),
                                                equalTo(STATUS, "200")),
                                point -> point.hasCount(2)
                                        .hasAttributesSatisfying(
                                                equalTo(URI, "/vertx/echo/{msg}"),
                                                equalTo(METHOD, "GET"),
                                                equalTo(STATUS, "200")),
                                point -> point.hasCount(5) // 3 x 200 + 400 + 500 status codes
                                        .hasAttributesSatisfying(
                                                equalTo(URI, "/ping/{message}"),
                                                equalTo(METHOD, "GET"),
                                                equalTo(STATUS, "200")),
                                point -> point.hasCount(3)
                                        .hasAttributesSatisfying(
                                                equalTo(URI, "/async-ping/{message}"),
                                                equalTo(METHOD, "GET"),
                                                equalTo(STATUS, "200")),
                                point -> point.hasCount(6) // 3 sync requests and 3 async requests
                                        .hasAttributesSatisfying(
                                                equalTo(URI, "/pong/{message}"),
                                                equalTo(METHOD, "GET"),
                                                equalTo(STATUS, "200")),
                                point -> point.hasCount(1)
                                        .hasAttributesSatisfying(
                                                equalTo(URI, "/pong/{message}"),
                                                equalTo(METHOD, "GET"),
                                                equalTo(STATUS, "500")),
                                point -> point.hasCount(1)
                                        .hasAttributesSatisfying(
                                                equalTo(URI, "/pong/{message}"),
                                                equalTo(METHOD, "GET"),
                                                equalTo(STATUS, "400"))));
    }
}
