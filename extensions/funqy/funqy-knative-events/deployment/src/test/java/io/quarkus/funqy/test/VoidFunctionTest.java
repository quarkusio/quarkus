package io.quarkus.funqy.test;

import static io.quarkus.funqy.test.VoidFunction.TEST_EXCEPTION_MSG;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.funqy.runtime.ApplicationException;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class VoidFunctionTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("void-function.properties", "application.properties")
                    .addClasses(VoidFunction.class));

    @Test
    public void testBinary() {
        RestAssured.given().contentType("application/json")
                .body("false")
                .header("ce-id", "1234")
                .header("ce-specversion", "1.0")
                .post("/")
                .then().statusCode(204);
    }

    @Test
    public void testBinaryException() {
        RestAssured.given().contentType("application/json")
                .body("true")
                .header("ce-id", "1234")
                .header("ce-specversion", "1.0")
                .post("/")
                .then()
                .statusCode(500)
                .body(allOf(containsString(TEST_EXCEPTION_MSG), containsString(ApplicationException.class.getName())));
    }

    static final String eventFmt = "{ \"id\" : \"1234\", " +
            "  \"specversion\": \"1.0\", " +
            "  \"source\": \"/foo\", " +
            "  \"type\": \"sometype\", " +
            "  \"datacontenttype\": \"application/json\", " +
            "  \"data\": %s " +
            "}";

    @Test
    public void testStructured() {
        RestAssured.given().contentType("application/cloudevents+json")
                .body(String.format(eventFmt, "false"))
                .post("/")
                .then().statusCode(204);
    }

    @Test
    public void testStructuredException() {
        RestAssured.given().contentType("application/cloudevents+json")
                .body(String.format(eventFmt, "true"))
                .post("/")
                .then()
                .statusCode(500)
                .body(allOf(containsString(TEST_EXCEPTION_MSG), containsString(ApplicationException.class.getName())));
    }

}
