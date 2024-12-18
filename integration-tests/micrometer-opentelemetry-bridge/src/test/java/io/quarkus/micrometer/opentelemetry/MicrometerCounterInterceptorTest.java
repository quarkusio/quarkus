package io.quarkus.micrometer.opentelemetry;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.attributeEntry;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.sdk.metrics.data.MetricData;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class MicrometerCounterInterceptorTest {

    @BeforeEach
    @AfterEach
    void reset() {
        given().get("/reset").then().statusCode(HTTP_OK);
    }

    @Test
    void testCountAllMetrics_MetricsOnSuccess() {
        given()
                .when()
                .get("/count")
                .then()
                .statusCode(200);

        await().atMost(5, SECONDS).until(() -> getMetrics("metric.all").size() > 1);

        List<Map<String, Object>> metrics = getMetrics("metric.all");

        //        Counter counter = registry.get("metric.all")
        //                .tag("method", "countAllInvocations")
        //                .tag("class", "io.smallrye.opentelemetry.implementation.micrometer.cdi.CountedBean")
        //                .tag("extra", "tag")
        //                //                .tag("do_fail", "prefix_true") // FIXME @MeterTag not implemented yet
        //                .tag("result", "success").counter();
        //        assertNotNull(counter);

//        MetricData metricAll = metrics.get(metrics.size() - 1); // get last
//        assertThat(metricAll)
//                .isNotNull()
//                .hasName("metric.all")
//                .hasDescription("Total number of invocations for method")
//                .hasUnit("invocations")
//                .hasDoubleSumSatisfying(sum -> sum.hasPointsSatisfying(point -> point.hasValue(1)
//                        .hasAttributes(attributeEntry(
//                                "class",
//                                "io.smallrye.opentelemetry.implementation.micrometer.cdi.CountedBean"),
//                                attributeEntry("method", "countAllInvocations"),
//                                attributeEntry("extra", "tag"),
//                                attributeEntry("exception", "none"),
//                                attributeEntry("result", "success"))));
    }

    //    @Test
    //    void testCountAllMetrics_MetricsOnFailure() {
    //        Assertions.assertThrows(NullPointerException.class, () -> countedBean.countAllInvocations(true));
    //
    //        Counter counter = registry.get("metric.all")
    //                .tag("method", "countAllInvocations")
    //                .tag("class", "io.smallrye.opentelemetry.implementation.micrometer.cdi.CountedBean")
    //                .tag("extra", "tag")
    //                //                .tag("do_fail", "prefix_true") // FIXME @MeterTag not implemented yet
    //                .tag("exception", "NullPointerException")
    //                .tag("result", "failure").counter();
    //        assertNotNull(counter);
    //
    //        MetricData metricAll = exporter.getFinishedMetricItem("metric.all");
    //        assertThat(metricAll)
    //                .isNotNull()
    //                .hasName("metric.all")
    //                .hasDescription("Total number of invocations for method")
    //                .hasUnit("invocations")
    //                .hasDoubleSumSatisfying(sum -> sum.hasPointsSatisfying(point -> point.hasValue(1)
    //                        .hasAttributes(attributeEntry(
    //                                "class",
    //                                "io.smallrye.opentelemetry.implementation.micrometer.cdi.CountedBean"),
    //                                attributeEntry("method", "countAllInvocations"),
    //                                attributeEntry("extra", "tag"),
    //                                attributeEntry("exception", "NullPointerException"),
    //                                attributeEntry("result", "failure"))));
    //    }

    private List<Map<String, Object>> getMetrics(String metricName) {
        return given()
                .when()
                .queryParam("name", metricName)
                .get("/export/metrics")
                .body().as(new TypeRef<>() {
                });
    }

}
