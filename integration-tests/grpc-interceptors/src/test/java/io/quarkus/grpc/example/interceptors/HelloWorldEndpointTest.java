package io.quarkus.grpc.example.interceptors;

import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

@QuarkusTest
class HelloWorldEndpointTest {

    @Test
    public void testHelloWorldServiceUsingBlockingStub() {
        Response response = get("/hello/blocking/neo");
        String intercepted = response.getHeader("intercepted");
        String responseMsg = response.asString();
        assertThat(responseMsg).isEqualTo("Hello neo");
        assertThat(intercepted).isEqualTo("true");

        ensureThatMetricsAreProduced();
    }

    @Test
    public void testHelloWorldServiceUsingMutinyStub() {
        String response = get("/hello/mutiny/neo-mutiny").asString();
        assertThat(response).isEqualTo("Hello neo-mutiny");
    }

    public void ensureThatMetricsAreProduced() {
        String metrics = get("/q/metrics")
                .then().statusCode(200)
                .extract().asString();

        assertThat(metrics)
                .contains("grpc_server_processing_duration_seconds_max") // server
                .contains("grpc_client_processing_duration_seconds_count"); // client
    }

}
