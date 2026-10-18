package io.quarkus.resteasy.test;

import static org.hamcrest.Matchers.is;

import java.util.function.Function;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class RestEasyDevModeTestCase {

    @RegisterExtension
    public static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(PostResource.class)
                    .addClass(GreetingResource.class)
                    .addClass(InterfaceResource.class)
                    .addClass(InterfaceResourceImpl.class)
                    .addClass(Service.class)
                    .addAsResource("config-test.properties", "application.properties"));

    @Test
    public void testRESTEasyHotReplacement() {
        RestAssured.given().body("Stuart")
                .when()
                .post("/post")
                .then()
                .body(Matchers.equalTo("Hello: Stuart"));
        test.modifySourceFile(PostResource.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("Hello:", "Hi:");
            }
        });
        RestAssured.given().body("Stuart")
                .when()
                .post("/post")
                .then()
                .body(Matchers.equalTo("Hi: Stuart"));
    }

    @Test
    public void testConfigHotReplacement() {
        RestAssured.when().get("/greeting").then()
                .statusCode(200)
                .body(is("hello from dev mode"));

        test.modifyResourceFile("application.properties", s -> s.replace("hello", "hi"));

        RestAssured.when().get("/greeting").then()
                .statusCode(200)
                .body(is("hi from dev mode"));
    }

    @Test
    public void testInterfaceImplementation() {
        RestAssured.when().get("/inter/hello").then()
                .statusCode(200)
                .body(is("hello from impl"));
    }
}
