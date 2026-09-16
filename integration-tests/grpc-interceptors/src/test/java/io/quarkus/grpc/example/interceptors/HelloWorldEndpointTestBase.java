package io.quarkus.grpc.example.interceptors;

import static io.quarkus.test.micrometer.PrometheusMetricsAssert.assertMetrics;
import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

class HelloWorldEndpointTestBase {

    @Test
    public void testHelloWorldServiceUsingBlockingStub() {
        Response response = get("/hello/blocking/neo");
        String responseMsg = response.asString();
        assertThat(responseMsg).isEqualTo("Hello neo");

        Set<String> invoked = Set.of(response.getHeader("interceptors").split(","));
        assertThat(invoked).containsExactlyInAnyOrder(
                "io.quarkus.grpc.examples.interceptors.ClientInterceptors$TypeTarget",
                "io.quarkus.grpc.examples.interceptors.ClientInterceptors$MethodTarget",
                "io.quarkus.grpc.examples.interceptors.PriorityImplInterceptor",
                "io.quarkus.grpc.examples.interceptors.ServerInterceptors$TypeTarget",
                "io.quarkus.grpc.examples.interceptors.ServerInterceptors$MethodTarget");

        ensureThatMetricsAreProduced();

        String used = response.getHeader("used_cbc");
        assertThat(Boolean.parseBoolean(used)).isTrue();

        used = response.getHeader("used_sbc");
        assertThat(Boolean.parseBoolean(used)).isTrue();
    }

    @Test
    public void testHelloWorldServiceUsingMutinyStub() {
        String response = get("/hello/mutiny/neo-mutiny").asString();
        assertThat(response).isEqualTo("Hello neo-mutiny");
    }

    @Test
    public void testRestClientThenGrpcDoesNotConflictWithMetrics() {
        String response = get("/hello/rest-then-grpc/neo").then().statusCode(200)
                .extract().asString();
        assertThat(response).isEqualTo("pong Hello neo");

        assertMetrics(get("/q/metrics").then().statusCode(200)
                .extract().asInputStream())
                .hasMetricNameContaining("http_client_requests_seconds");
    }

    public void ensureThatMetricsAreProduced() {
        assertMetrics(get("/q/metrics").then().statusCode(200)
                .extract().asInputStream())
                .hasMetricNameContaining("grpc_server_processing_duration_seconds_max") // server
                .hasMetricNameContaining("grpc_client_processing_duration_seconds_count"); // client
    }

}
