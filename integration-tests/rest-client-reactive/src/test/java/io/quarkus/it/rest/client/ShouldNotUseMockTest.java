package io.quarkus.it.rest.client;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@Order(200) // used in order to make sure this is run after InjectMockTest
public class ShouldNotUseMockTest {

    @Test
    void shouldMockClientInTheApp() {
        RestAssured.with().post("/call-cdi-client-with-exception-mapper")
                .then()
                .statusCode(200);
    }
}
