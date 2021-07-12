package io.quarkus.it.panache.reactive;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test various Panache operations running in Quarkus
 */
@QuarkusTest
public class PanacheFunctionalityTest {

    @Test
    public void tests() {
        RestAssured.when().get("/test/store3fruits").then().body(is("OK"));
        RestAssured.when().get("/test/load3fruits").then().body(is("OK"));
    }

}
