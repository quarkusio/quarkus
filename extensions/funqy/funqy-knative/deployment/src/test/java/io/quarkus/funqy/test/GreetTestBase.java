package io.quarkus.funqy.test;

import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

public abstract class GreetTestBase {

    protected abstract String getCeSource();

    protected abstract String getCeType();

    @Test
    public void testVanilla() {
        RestAssured.given().contentType("application/json")
                .body("{\"name\": \"Bill\"}")
                .post("/")
                .then().statusCode(200)
                .header("ce-id", nullValue())
                .body("name", equalTo("Bill"))
                .body("message", equalTo("Hello Bill!"));
    }

    @Test
    public void testVanillaNPE() {
        RestAssured.given().contentType("application/json")
                .body("null")
                .post("/")
                .then().statusCode(500);
    }

    @Test
    public void testBinary() {
        RestAssured.given().contentType("application/json")
                .body("{\"name\": \"Bill\"}")
                .header("ce-id", "1234")
                .header("ce-specversion", "1.0")
                .post("/")
                .then().statusCode(200)
                .header("ce-id", notNullValue())
                .header("ce-specversion", equalTo("1.0"))
                .header("ce-source", equalTo(getCeSource()))
                .header("ce-type", equalTo(getCeType()))
                .body("name", equalTo("Bill"))
                .body("message", equalTo("Hello Bill!"));
    }

    @Test
    public void testBinaryNPE() {
        RestAssured.given().contentType("application/json")
                .body("null")
                .header("ce-id", "1234")
                .header("ce-specversion", "1.0")
                .post("/")
                .then().statusCode(500);
    }

    static final String event = "{ \"id\" : \"1234\", " +
            "  \"specversion\": \"1.0\", " +
            "  \"source\": \"/foo\", " +
            "  \"type\": \"sometype\", " +
            "  \"datacontenttype\": \"application/json\", " +
            "  \"data\": { \"name\": \"Bill\" } " +
            "}";

    @Test
    public void testStructured() {
        RestAssured.given().contentType("application/cloudevents+json")
                .body(event)
                .post("/")
                .then().statusCode(200)
                .defaultParser(Parser.JSON)
                .body("id", notNullValue())
                .body("specversion", equalTo("1.0"))
                .body("type", equalTo(getCeType()))
                .body("source", equalTo(getCeSource()))
                .body("datacontenttype", equalTo("application/json"))
                .body("data.name", equalTo("Bill"))
                .body("data.message", equalTo("Hello Bill!"));
    }

    static final String eventWithNullData = "{ \"id\" : \"1234\", " +
            "  \"specversion\": \"1.0\", " +
            "  \"source\": \"/foo\", " +
            "  \"type\": \"sometype\", " +
            "  \"datacontenttype\": \"application/json\", " +
            "  \"data\": null " +
            "}";

    @Test
    public void testStructuredNPE() {
        RestAssured.given().contentType("application/cloudevents+json")
                .body(eventWithNullData)
                .post("/")
                .then().statusCode(500);
    }

}
