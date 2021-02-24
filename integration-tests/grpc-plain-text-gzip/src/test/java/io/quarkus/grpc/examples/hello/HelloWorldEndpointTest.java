package io.quarkus.grpc.examples.hello;

import static io.restassured.RestAssured.delete;
import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class HelloWorldEndpointTest {

    @BeforeEach
    @AfterEach
    public void cleanUp() {
        delete("/hello/encoding").then().statusCode(204);
    }

    @Test
    public void testHelloWorldServiceUsingBlockingStub() {
        String response = get("/hello/blocking/neo").asString();
        assertThat(response).startsWith("Hello neo");
        assertThat(get("/hello/encoding").asString()).isEqualTo("gzip");
    }

    @Test
    public void testHelloWorldServiceUsingMutinyStub() {
        String response = get("/hello/mutiny/neo-mutiny").asString();
        assertThat(response).startsWith("Hello neo-mutiny");
        assertThat(get("/hello/encoding").asString()).isEqualTo("gzip");
    }

}
