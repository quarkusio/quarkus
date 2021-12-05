package io.quarkus.funqy.test;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class WithAttributeFilterTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WithAttributeFilter.class, Identity.class));

    @Test
    public void testAttributeFilterMatch() {
        RestAssured.given().contentType("application/json")
                .body("[{\"name\": \"Bill\"}, {\"name\": \"Matej\"}]")
                .header("ce-id", "42")
                .header("ce-type", "listOfStrings")
                .header("ce-source", "testvalue")
                .header("ce-specversion", "1.0")
                .post("/")
                .then().statusCode(200)
                .body(equalTo("\"Bill,Matej\""));
    }

    @Test
    public void testAttributeFilterMatchOther() {
        RestAssured.given().contentType("application/json")
                .body("[{\"name\": \"Bill\"}, {\"name\": \"Matej\"}]")
                .header("ce-id", "42")
                .header("ce-type", "listOfStrings")
                .header("ce-source", "test2")
                .header("ce-specversion", "1.0")
                .post("/")
                .then().statusCode(200)
                .body(equalTo("\"Bill;Matej\""));
    }

    @Test
    public void testAttributeFilterDoesNotMatch() {
        RestAssured.given().contentType("application/json")
                .body("[{\"name\": \"Bill\"}, {\"name\": \"Matej\"}]")
                .header("ce-id", "42")
                .header("ce-type", "listOfStrings")
                .header("ce-source", "another")
                .header("ce-specversion", "1.0")
                .post("/")
                .then().statusCode(404);
    }

    @Test
    public void testAttributeFiltersAllMatch() {
        RestAssured.given().contentType("application/json")
                .body("3")
                .header("ce-id", "42")
                .header("ce-type", "integer")
                .header("ce-source", "test")
                .header("ce-specversion", "1.0")
                .header("ce-custom", "hello")
                .post("/")
                .then().statusCode(200);
    }

    @Test
    public void testAttributeFiltersNotAllMatch() {
        RestAssured.given().contentType("application/json")
                .body("3")
                .header("ce-id", "42")
                .header("ce-type", "integer")
                .header("ce-source", "test")
                .header("ce-specversion", "1.0")
                .header("ce-custom", "bye")
                .post("/")
                .then().statusCode(404);
    }

    @Test
    public void testAttributeFiltersConflict() {
        RestAssured.given().contentType("application/json")
                .body("[{\"name\": \"Bill\"}, {\"name\": \"Matej\"}]")
                .header("ce-id", "42")
                .header("ce-type", "listOfStrings")
                .header("ce-source", "test")
                .header("ce-specversion", "1.0")
                .header("ce-customA", "value")
                .header("ce-customB", "value")
                .post("/")
                .then().statusCode(409);
    }

    @Test
    public void testAttributeFiltersCEAndCustomAttributes() {
        RestAssured.given().contentType("application/json")
                .body("[{\"name\": \"Bill\"}, {\"name\": \"Matej\"}]")
                .header("ce-id", "42")
                .header("ce-type", "listOfStrings")
                .header("ce-source", "test")
                .header("ce-specversion", "1.0")
                .header("ce-custom", "value")
                .post("/")
                .then().statusCode(200);
    }

    @Test
    public void testAttributeFiltersSameAttributesDifferentValue() {
        RestAssured.given().contentType("application/json")
                .body("[{\"name\": \"Bill\"}, {\"name\": \"Matej\"}]")
                .header("ce-id", "42")
                .header("ce-type", "listOfStrings")
                .header("ce-source", "test")
                .header("ce-specversion", "1.0")
                .header("ce-customA", "value")
                .post("/")
                .then().statusCode(200)
                .body(equalTo("\"value\""));
    }

    @Test
    public void testAttributeFiltersSameAttributesDifferentValue2() {
        RestAssured.given().contentType("application/json")
                .body("[{\"name\": \"Bill\"}, {\"name\": \"Matej\"}]")
                .header("ce-id", "42")
                .header("ce-type", "listOfStrings")
                .header("ce-source", "test")
                .header("ce-specversion", "1.0")
                .header("ce-customB", "value")
                .post("/")
                .then().statusCode(200)
                .body(equalTo("\"someOtherValue\""));
    }

    static final String event1 = "{ \"id\" : \"1234\", " +
            "  \"specversion\": \"1.0\", " +
            "  \"source\": \"testvalue\", " +
            "  \"type\": \"listOfStrings\", " +
            "  \"datacontenttype\": \"application/json\", " +
            "  \"data\": [{\"name\": \"Bill\"}, {\"name\": \"Matej\"}] " +
            "}";
    static final String event2 = "{ \"id\" : \"1234\", " +
            "  \"specversion\": \"1.0\", " +
            "  \"source\": \"test\", " +
            "  \"type\": \"integer\", " +
            "  \"custom\": \"hello\", " +
            "  \"datacontenttype\": \"application/json\", " +
            "  \"data\": 3 " +
            "}";
    static final String event3 = "{ \"id\" : \"1234\", " +
            "  \"specversion\": \"1.0\", " +
            "  \"source\": \"another\", " +
            "  \"type\": \"listOfStrings\", " +
            "  \"datacontenttype\": \"application/json\", " +
            "  \"data\": [{\"name\": \"Bill\"}, {\"name\": \"Matej\"}] " +
            "}";
    static final String event4 = "{ \"id\" : \"1234\", " +
            "  \"specversion\": \"1.0\", " +
            "  \"source\": \"test\", " +
            "  \"type\": \"integer\", " +
            "  \"custom\": \"bye\", " +
            "  \"datacontenttype\": \"application/json\", " +
            "  \"data\": 3 " +
            "}";

    static final String event5 = "{ \"id\" : \"1234\", " +
            "  \"specversion\": \"1.0\", " +
            "  \"source\": \"test\", " +
            "  \"type\": \"listOfStrings\", " +
            "  \"customA\": \"value\", " +
            "  \"customB\": \"value\", " +
            "  \"datacontenttype\": \"application/json\", " +
            "  \"data\": [{\"name\": \"Bill\"}, {\"name\": \"Matej\"}] " +
            "}";

    static final String event6 = "{ \"id\" : \"1234\", " +
            "  \"specversion\": \"1.0\", " +
            "  \"source\": \"test\", " +
            "  \"type\": \"listOfStrings\", " +
            "  \"custom\": \"value\", " +
            "  \"datacontenttype\": \"application/json\", " +
            "  \"data\": [{\"name\": \"Bill\"}, {\"name\": \"Matej\"}] " +
            "}";

    static final String event7 = "{ \"id\" : \"1234\", " +
            "  \"specversion\": \"1.0\", " +
            "  \"source\": \"test\", " +
            "  \"type\": \"listOfStrings\", " +
            "  \"customA\": \"value\", " +
            "  \"datacontenttype\": \"application/json\", " +
            "  \"data\": [{\"name\": \"Bill\"}, {\"name\": \"Matej\"}] " +
            "}";

    static final String event8 = "{ \"id\" : \"1234\", " +
            "  \"specversion\": \"1.0\", " +
            "  \"source\": \"test\", " +
            "  \"type\": \"listOfStrings\", " +
            "  \"customB\": \"value\", " +
            "  \"datacontenttype\": \"application/json\", " +
            "  \"data\": [{\"name\": \"Bill\"}, {\"name\": \"Matej\"}] " +
            "}";

    @Test
    public void testAttributeFilterMatchStructured() {
        RestAssured.given().contentType("application/cloudevents+json")
                .body(event1)
                .post("/")
                .then().statusCode(200)
                .body("data", equalTo("Bill,Matej"));
    }

    @Test
    public void testAttributeFilterDoesNotMatchStructured() {
        RestAssured.given().contentType("application/cloudevents+json")
                .body(event3)
                .post("/")
                .then().statusCode(404);
    }

    @Test
    public void testAttributeFiltersAllMatchStructured() {
        RestAssured.given().contentType("application/cloudevents+json")
                .body(event2)
                .post("/")
                .then().statusCode(200);
    }

    @Test
    public void testAttributeFiltersNotAllMatchStructured() {
        RestAssured.given().contentType("application/cloudevents+json")
                .body(event4)
                .post("/")
                .then().statusCode(404);
    }

    @Test
    public void testAttributeFiltersConflictStructured() {
        RestAssured.given().contentType("application/cloudevents+json")
                .body(event5)
                .post("/")
                .then().statusCode(409);
    }

    @Test
    public void testAttributeFiltersCEAndCustomAttributesStructured() {
        RestAssured.given().contentType("application/cloudevents+json")
                .body(event6)
                .post("/")
                .then().statusCode(200);
    }

    @Test
    public void testAttributeFiltersSameAttributesDifferentValueStructured() {
        RestAssured.given().contentType("application/cloudevents+json")
                .body(event7)
                .post("/")
                .then().statusCode(200);
    }

    @Test
    public void testAttributeFiltersSameAttributesDifferentValue2Structured() {
        RestAssured.given().contentType("application/cloudevents+json")
                .body(event8)
                .post("/")
                .then().statusCode(200);
    }
}
