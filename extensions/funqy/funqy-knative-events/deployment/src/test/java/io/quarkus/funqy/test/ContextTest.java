package io.quarkus.funqy.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ContextTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ContextFunction.class));

    @Test
    public void testMapping() {
        RestAssured.given().contentType("application/json")
                .header("ce-id", "1234")
                .header("ce-specversion", "1.0")
                .header("ce-type", "context")
                .header("ce-source", "test")
                .header("ce-subject", "bb")
                .header("ce-time", "2018-04-05T17:31:00Z")
                .body("\"HELLO\"")
                .post("/")
                .then().statusCode(204);
    }

    static final String contextEvent = "{ \"id\" : \"1234\", " +
            "  \"specversion\": \"1.0\", " +
            "  \"source\": \"test\", " +
            "  \"subject\": \"bb\", " +
            "  \"time\": \"2018-04-05T17:31:00Z\", " +
            "  \"type\": \"context\", " +
            "  \"datacontenttype\": \"application/json\", " +
            "  \"data\": \"HELLO\" " +
            "}";

    @Test
    public void testStructuredMapping() {
        RestAssured.given().contentType("application/cloudevents+json")
                .body(contextEvent)
                .post("/")
                .then().statusCode(204);
    }

}
