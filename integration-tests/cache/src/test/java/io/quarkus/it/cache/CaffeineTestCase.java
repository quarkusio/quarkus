package io.quarkus.it.cache;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class CaffeineTestCase {

    @Test
    public void test() {
        given().when().get("/caffeine/hasLoadAll").then().statusCode(200).body(
                "loader", is(false),
                "bulk-loader", is(true));
    }
}
