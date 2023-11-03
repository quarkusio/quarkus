package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class SimpleBeanTestCase {

    @Test
    public void testRequestScope() {
        RestAssured.when().get("/simple-bean").then()
                .body(is("OK"));
    }

}
