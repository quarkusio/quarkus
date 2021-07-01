package io.quarkus.it.panache;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class PanacheRepositoryBaseTest {

    @Test
    public void test() {
        RestAssured.when().get("/users/1").then().statusCode(404);
    }
}
