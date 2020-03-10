package io.quarkus.funqy.test;

import static org.hamcrest.Matchers.*;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

public class GreetTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("greeting.properties", "application.properties")
                    .addClasses(PrimitiveFunctions.class, GreetingFunctions.class, Greeting.class, GreetingService.class,
                            Identity.class));

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
    public void testBinary() {
        RestAssured.given().contentType("application/json")
                .body("{\"name\": \"Bill\"}")
                .header("ce-id", "1234")
                .header("ce-specversion", "1.0")
                .post("/")
                .then().statusCode(200)
                .header("ce-id", notNullValue())
                .header("ce-specversion", equalTo("1.0"))
                .header("ce-source", equalTo("dev.knative.greet"))
                .header("ce-type", equalTo("greet"))
                .body("name", equalTo("Bill"))
                .body("message", equalTo("Hello Bill!"));
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
                .body("type", equalTo("greet"))
                .body("source", equalTo("dev.knative.greet"))
                .body("datacontenttype", equalTo("application/json"))
                .body("data.name", equalTo("Bill"))
                .body("data.message", equalTo("Hello Bill!"));
    }

}
