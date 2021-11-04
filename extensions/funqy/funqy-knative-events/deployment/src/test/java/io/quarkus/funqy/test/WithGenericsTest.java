package io.quarkus.funqy.test;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class WithGenericsTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WithGenerics.class, Identity.class));

    @Test
    public void testToCommaSeparated() {
        RestAssured.given().contentType("application/json")
                .body("[{\"name\": \"Bill\"}, {\"name\": \"Matej\"}]")
                .header("ce-id", "42")
                .header("ce-type", "listOfStrings")
                .header("ce-source", "/ofTest")
                .header("ce-specversion", "1.0")
                .post("/")
                .then().statusCode(200)
                .body(equalTo("\"Bill,Matej\""));
    }

    @Test
    public void testRange() {
        RestAssured.given().contentType("application/json")
                .body("3")
                .header("ce-id", "42")
                .header("ce-type", "integer")
                .header("ce-source", "/ofTest")
                .header("ce-specversion", "1.0")
                .post("/")
                .then().statusCode(200);
    }
}
