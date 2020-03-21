package io.quarkus.it.arango;

import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest()
public class ArangoFunctionalityTest {

    @Test
    public void it_should_return_arango_version() {
        RestAssured.given().when().get("/arango/version").then().body(is("3.6.2"));
    }

    @Test
    public void it_should_return_arango_db() throws InterruptedException {
        RestAssured.given().when().get("/arango/db").then().body(is("db"));
    }
}
