package io.quarkus.funqy.test;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OverloadingTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().withApplicationRoot((jar) -> jar.addClasses(Overloading.class));

    @Test
    public void testMapping() {
        RestAssured.given().contentType("application/json").body("\"a\"").post("/strfun").then().statusCode(200)
                .body(equalTo("\"A\""));

        RestAssured.given().contentType("application/json").body("10").post("/intfun").then().statusCode(200)
                .body(equalTo("20"));
    }
}
