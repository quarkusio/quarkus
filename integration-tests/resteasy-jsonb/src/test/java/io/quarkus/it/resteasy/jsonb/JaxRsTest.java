package io.quarkus.it.resteasy.jsonb;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.StringContains.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class JaxRsTest {

    @Test
    public void testComplexObject() {
        RestAssured.when().get("/coffee").then()
                .statusCode(200)
                .body(containsString("Robusta"), containsString("Ethiopia"));
    }

    @Test
    public void testImplementationClass() {
        RestAssured.when().get("/hasName").then()
                .statusCode(200)
                .body(is("{\"age\":40,\"name\":\"Alice\"}"));
    }

    @Test
    public void testJaxRsResourceResult() {
        RestAssured.when().get("/cat").then()
                .statusCode(200)
                .body(is("[{\"age\":\"1.00\",\"color\":\"Grey\",\"breed\":\"Scottish Fold\"}]"));
    }

    @Test
    public void testPojoThatHasNoSerializer() {
        RestAssured.when().get("/greeter").then()
                .statusCode(200)
                .body(is("{\"message\":\"hello\"}"));
    }
}
