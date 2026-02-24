package io.quarkus.it.rest.client;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class RedirectResourceTest {

    @Test
    void test() {
        when()
                .get("/redirect")
                .then()
                .statusCode(200)
                .body(equalTo("other"));
    }
}
