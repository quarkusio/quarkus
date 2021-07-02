package io.quarkus.it.resteasy.reactive;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class BigKeysResourceTest {

    @Test
    public void test() throws IOException {
        given()
                .contentType("application/json")
                .accept("application/json")
                .body("{\"bdMap\":{\"1\":\"One\", \"2\":\"Two\"},  \"biMap\":{\"1\":\"One\", \"2\":\"Two\"}}")
                .when().post("/bigkeys")
                .then()
                .statusCode(200)
                .body("bdMap.1", equalTo("One"))
                .body("biMap.2", equalTo("Two"));
    }

}
