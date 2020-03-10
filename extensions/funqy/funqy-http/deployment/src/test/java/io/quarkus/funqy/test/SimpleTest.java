package io.quarkus.funqy.test;

import static org.hamcrest.Matchers.equalTo;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class SimpleTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(PrimitiveFunctions.class, GreetingFunctions.class, Greeting.class, GreetingService.class,
                            GreetingTemplate.class));

    @Test
    public void testString() {
        RestAssured.given().contentType("application/json").body("\"Hello\"").post("/toLowerCase")
                .then().statusCode(200).body(equalTo("\"hello\""));
    }

    @Test
    public void testInt() {
        RestAssured.given().contentType("application/json").body("2").post("/doubleIt")
                .then().statusCode(200).body(equalTo("4"));
    }

    @Test
    public void testObject() {
        RestAssured.given().contentType("application/json")
                .body("\"Bill\"")
                .post("/greet")
                .then().statusCode(200)
                .body(equalTo("{\"name\":\"Bill\",\"message\":\"Hello Bill!\"}"));

        RestAssured.given().contentType("application/json")
                .body("{\"greeting\":\"Guten tag\",\"punctuation\":\".\"}")
                .post("/template")
                .then().statusCode(200);

        RestAssured.given().contentType("application/json")
                .body("\"Bill\"")
                .post("/greet")
                .then().statusCode(200)
                .body(equalTo("{\"name\":\"Bill\",\"message\":\"Guten tag Bill.\"}"));

    }

}
