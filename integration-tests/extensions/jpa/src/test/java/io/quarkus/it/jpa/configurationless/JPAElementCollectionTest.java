package io.quarkus.it.jpa.configurationless;

import static org.hamcrest.core.StringContains.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class JPAElementCollectionTest {

    @Test
    public void testInjection() {
        RestAssured.when().get("/jpa-test/element-collection").then()
                .statusCode(200)
                .body(containsString("OK"));
    }
}
