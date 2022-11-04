package io.quarkus.grpc.examples.hello;

import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HelloWorldEndpointTestBase {

    @Test
    public void testHelloWorldServiceUsingBlockingStub() {
        String response = get("/hello/blocking/neo").asString();
        assertThat(response).startsWith("Hello neo");
    }

    @Test
    public void testHelloWorldServiceUsingMutinyStub() {
        String response = get("/hello/mutiny/neo-mutiny").asString();
        assertThat(response).startsWith("Hello neo-mutiny");
    }

}
