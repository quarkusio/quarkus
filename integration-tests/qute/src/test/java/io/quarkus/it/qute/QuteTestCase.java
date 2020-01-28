package io.quarkus.it.qute;

import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class QuteTestCase {

    @Test
    public void testTemplate() throws InterruptedException {
        RestAssured.when().get("/hello?name=Ciri").then()
                .body(containsString("Hello Ciri!"));
        RestAssured.when().get("/hello").then()
                .body(containsString("Hello world!"));

    }

}
