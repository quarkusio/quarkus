package io.quarkus.grpc.examples.hello;

import static io.restassured.RestAssured.delete;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class HelloWorldEndpointTest {

    @Test
    public void testHelloWorldServiceUsingBlockingStub() {
        String response = get("/hello/blocking/neo").asString();
        assertThat(response).startsWith("Hello neo");

        assertNoHeaders();
    }

    @Test
    public void testHelloWorldServiceUsingMutinyStub() {
        String response = get("/hello/mutiny/neo-mutiny").asString();
        assertThat(response).startsWith("Hello neo-mutiny");
        assertNoHeaders();
    }

    @Test
    void shouldSetHeaderWithMutiny() {
        String response = given().queryParam("headers", "true")
                .when().get("/hello/mutiny/neo-mutiny-w-headers").asString();
        assertThat(response).startsWith("Hello neo-mutiny-w-headers");
        assertHasHeader("my-extra-header", "my-extra-value");
    }

    @Test
    void shouldSetHeader() {
        String response = given().queryParam("headers", "true")
                .when().get("/hello/blocking/neo-w-headers").asString();
        assertThat(response).startsWith("Hello neo-w-headers");
        assertHasHeader("my-blocking-header", "my-blocking-value");
    }

    @Test
    void shouldSetHeaderWithInterface() {
        String response = given().queryParam("headers", "true")
                .when().get("/hello/interface/i-neo-w-headers").asString();
        assertThat(response).startsWith("Hello i-neo-w-headers");
        assertHasHeader("my-interface-header", "my-interface-value");
    }

    @BeforeEach
    public void setUp() {
        delete("/hello").then().statusCode(204);
    }

    private void assertHasHeader(String key, String value) {
        Map<?, ?> result = get("/hello/headers").as(Map.class);
        assertThat(result).hasSize(1);
        assertThat(result.get(key)).isEqualTo(value);
    }

    private void assertNoHeaders() {
        Map<?, ?> result = get("/hello/headers").as(Map.class);
        assertThat(result).hasSize(0);
    }

}
