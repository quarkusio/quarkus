package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;

public abstract class CommonNestedTest {

    public String defaultProfile() {
        return "Hello";
    }

    @Nested
    class NestedTests {
        @Test
        public void testProfileFromNested() {
            RestAssured.when()
                    .get("/greeting/Stu")
                    .then()
                    .statusCode(200)
                    .body(is(defaultProfile() + " Stu"));
        }
    }
}
