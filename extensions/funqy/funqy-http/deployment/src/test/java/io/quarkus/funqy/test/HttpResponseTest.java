package io.quarkus.funqy.test;

import static io.quarkus.funqy.test.HttpResponseFunctions.NOT_FOUND_MESSAGE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class HttpResponseTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Greeting.class, HttpResponseFunctions.class));

    @Test
    void testFunqResponseCodes() {
        RestAssured.given()
                .contentType("application/json")
                .body("\"Henry\"")
                .post("/voidFunqResponseSuccess")
                .then().statusCode(200)
                .body(equalTo("{\"name\":\"Henry\",\"message\":\"Hi Henry\"}"));

        RestAssured.given()
                .contentType("application/json")
                .body("\"Bill\"")
                .post("/voidFunqResponseCreated")
                .then().statusCode(201)
                .body(equalTo("{\"name\":\"Bill\",\"message\":\"Created user successfully\"}"));

        RestAssured.given()
                .get("/voidFunqResponseNotFound")
                .then()
                .statusCode(404)
                .body(containsString(NOT_FOUND_MESSAGE));
    }
}