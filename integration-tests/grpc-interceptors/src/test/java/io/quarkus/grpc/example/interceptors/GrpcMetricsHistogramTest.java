package io.quarkus.grpc.example.interceptors;

import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(GrpcMetricsHistogramTestProfile.class)
class GrpcMetricsHistogramTest {

    @Test
    void processingDurationPublishesHistogramBuckets() {
        get("/hello/blocking/neo").then().statusCode(200);

        String metrics = get("/q/metrics").then().statusCode(200).extract().asString();

        assertThat(metrics)
                .contains("# TYPE grpc_server_processing_duration_seconds histogram")
                .contains("grpc_server_processing_duration_seconds_bucket{")
                .contains("le=\"0.005\"")
                .contains("le=\"0.1\"")
                .contains("le=\"1.0\"")
                .contains("# TYPE grpc_client_processing_duration_seconds histogram")
                .contains("grpc_client_processing_duration_seconds_bucket{");
    }
}
