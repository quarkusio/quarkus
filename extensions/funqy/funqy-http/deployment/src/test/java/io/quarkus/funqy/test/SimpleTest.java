package io.quarkus.funqy.test;

import static org.hamcrest.Matchers.equalTo;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class SimpleTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
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
    @ValueSource(strings = { "/greet", "/greetAsync" })
    public void testObject(String path) {

        RestAssured.given().contentType("application/json")
                .body("{\"greeting\":\"Hello\",\"punctuation\":\"!\"}")
                .post("/template")
                .then().statusCode(200);

        RestAssured.given().contentType("application/json")
                .body("\"Bill\"")
                .post(path)
                .then().statusCode(200)
                .body(equalTo("{\"name\":\"Bill\",\"message\":\"Hello Bill!\"}"));

        RestAssured.given().contentType("application/json")
                .body("{\"greeting\":\"Guten tag\",\"punctuation\":\".\"}")
                .post("/template")
                .then().statusCode(200);

        RestAssured.given().contentType("application/json")
                .body("\"Bill\"")
                .post(path)
                .then().statusCode(200)
                .body(equalTo("{\"name\":\"Bill\",\"message\":\"Guten tag Bill.\"}"));

    }

}
