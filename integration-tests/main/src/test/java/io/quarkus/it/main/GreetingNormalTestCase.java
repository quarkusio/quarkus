package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class GreetingNormalTestCase {

    @Test
    public void included() {
        RestAssured.when()
                .get("/greeting/Stu")
                .then()
                .statusCode(200)
                .body(is("Hello Stu"));
    }
}
