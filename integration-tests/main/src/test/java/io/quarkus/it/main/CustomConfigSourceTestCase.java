package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class CustomConfigSourceTestCase {

    @Test
    public void testCustomConfig() {
        RestAssured.when().get("/core/config-test").then()
                .body(is("OK"));
    }
}
