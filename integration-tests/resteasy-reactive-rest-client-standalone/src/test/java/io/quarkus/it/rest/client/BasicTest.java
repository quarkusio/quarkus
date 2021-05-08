package io.quarkus.it.rest.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;

@QuarkusTest
public class BasicTest {

    @TestHTTPResource("/hello")
    String helloUrl;

    @Test
    public void shouldWork() {
        Response response = RestAssured.with().body(helloUrl).post("/call-client");
        assertThat(response.asString()).isEqualTo("Hello, JohnJohn");
    }
}
