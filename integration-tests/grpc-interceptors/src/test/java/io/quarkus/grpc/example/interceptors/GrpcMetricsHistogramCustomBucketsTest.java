package io.quarkus.grpc.example.interceptors;

import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(GrpcMetricsHistogramCustomBucketsTestProfile.class)
class GrpcMetricsHistogramCustomBucketsTest {

    private static final Pattern SERVER_BUCKETS = Pattern
            .compile("grpc_server_processing_duration_seconds_bucket\\{[^}]*le=\"([^\"]+)\"");
    private static final Pattern CLIENT_BUCKETS = Pattern
            .compile("grpc_client_processing_duration_seconds_bucket\\{[^}]*le=\"([^\"]+)\"");

    @Test
    void serverHistogramRespectsMinMaxAndCustomSlos() {
        get("/hello/blocking/neo").then().statusCode(200);

        String metrics = get("/q/metrics").then().statusCode(200).extract().asString();

        assertCustomBuckets(metrics, SERVER_BUCKETS, "grpc_server_processing_duration_seconds");
    }

    @Test
    void clientHistogramRespectsMinMaxAndCustomSlos() {
        get("/hello/blocking/neo").then().statusCode(200);

        String metrics = get("/q/metrics").then().statusCode(200).extract().asString();

        assertCustomBuckets(metrics, CLIENT_BUCKETS, "grpc_client_processing_duration_seconds");
    }

    private static void assertCustomBuckets(String metrics, Pattern bucketPattern, String metricName) {
        assertThat(metrics)
                .contains("# TYPE " + metricName + " histogram")
                .contains(metricName + "_bucket{");

        var bucketBounds = bucketPattern.matcher(metrics).results()
                .map(match -> match.group(1))
                .toList();

        assertThat(bucketBounds)
                .as("custom SLO buckets for %s", metricName)
                .contains("0.025", "0.05", "+Inf");

        // Micrometer's default percentile-histogram range is ~1ms..30s. With min=10ms / max=100ms,
        // buckets outside that clamped range (except +Inf and explicit SLOs) must not appear.
        assertThat(bucketBounds)
                .as("clamped bucket range for %s", metricName)
                .doesNotContain("0.001", "1.0", "10.0", "30.0");

        assertThat(bucketBounds.stream().filter(bound -> !"+Inf".equals(bound)).map(Double::parseDouble))
                .as("finite buckets for %s stay within the configured min/max window", metricName)
                .allSatisfy(bound -> assertThat(bound).isBetween(0.01, 0.1));
    }
}
