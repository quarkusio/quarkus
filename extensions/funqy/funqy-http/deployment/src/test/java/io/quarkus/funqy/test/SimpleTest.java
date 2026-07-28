package io.quarkus.funqy.test;

import static io.quarkus.funqy.test.PrimitiveFunctions.TEST_EXCEPTION_MSG;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.funqy.runtime.ApplicationException;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.vertx.core.json.JsonObject;

public class SimpleTest {
    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PrimitiveFunctions.class, GreetingFunctions.class, Greeting.class, GreetingService.class,
                            GreetingTemplate.class));

    @ParameterizedTest
    @ValueSource(strings = { "/toLowerCase", "/toLowerCaseAsync" })
    public void testString(String path) {
        RestAssured.given().contentType("application/json").body("\"Hello\"").post(path)
                .then().statusCode(200).body(equalTo("\"hello\""));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/doubleIt", "/doubleItAsync" })
    public void testInt(String path) {
        RestAssured.given().contentType("application/json").body("2").post(path)
                .then().statusCode(200).body(equalTo("4"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/noop", "/noopAsync" })
    public void testNoop(String path) {
        RestAssured.given().get(path)
                .then().statusCode(204);
        RestAssured.given().post(path)
                .then().statusCode(204);
    }

    @Test
    void testThrowException() {
        RestAssured.given().get("/voidFunThrowError")
                .then()
                .statusCode(500)
                .body(allOf(containsString(TEST_EXCEPTION_MSG), containsString(ApplicationException.class.getName())));
        RestAssured.given().post("/voidFunThrowError")
                .then()
                .statusCode(500)
                .body(allOf(containsString(TEST_EXCEPTION_MSG), containsString(ApplicationException.class.getName())));
    }

    @Test
    public void testGetOrPost() {
        RestAssured.given().get("/get")
                .then().statusCode(200).body(equalTo("\"get\""));
        RestAssured.given().post("/get")
                .then().statusCode(200).body(equalTo("\"get\""));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/greet", "/greetAsync" })
    public void testObject(String path) {

        RestAssured.given().contentType("application/json")
                .body("{\"greeting\":\"Hello\",\"punctuation\":\"!\"}")
                .post("/template")
                .then().statusCode(204);

        JsonObject expectedOutput = new JsonObject("{\"name\":\"Bill\",\"message\":\"Hello Bill!\"}");
        Map body = RestAssured.given().contentType("application/json")
                .body("\"Bill\"")
                .post(path)
                .then().statusCode(200)
                .extract().body().as(Map.class);
        Assertions.assertEquals(expectedOutput, new JsonObject(body));

        RestAssured.given().contentType("application/json")
                .body("{\"greeting\":\"Guten tag\",\"punctuation\":\".\"}")
                .post("/template")
                .then().statusCode(204);

        expectedOutput = new JsonObject("{\"name\":\"Bill\",\"message\":\"Guten tag Bill.\"}");
        body = RestAssured.given().contentType("application/json")
                .body("\"Bill\"")
                .post(path)
                .then().statusCode(200)
                .extract().body().as(Map.class);
        Assertions.assertEquals(expectedOutput, new JsonObject(body));

    }

}
