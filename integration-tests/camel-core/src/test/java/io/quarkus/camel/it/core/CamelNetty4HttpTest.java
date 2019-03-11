package io.quarkus.camel.it.core;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class CamelNetty4HttpTest {
    @Test
    public void testInvokeNetty4HttpEndpoint() {
        RestAssured
                .with()
                .baseUri("http://localhost:8999")
                .body("quarkus")
                .when()
                .post("/test")
                .then()
                .body(is("Hello quarkus"));
    }
}
