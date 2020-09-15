package org.acme.qute;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ItemsResourceTest {

    @Test
    public void testEndpoint() {
        given()
                .when().get("/qute/items")
                .then()
                .statusCode(200)
                .body(containsString("Apple:"), containsString("<del>30</del> <strong>27.0</strong>"));
    }

}
