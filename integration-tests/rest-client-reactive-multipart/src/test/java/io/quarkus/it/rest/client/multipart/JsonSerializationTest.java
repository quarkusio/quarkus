package io.quarkus.it.rest.client.multipart;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class JsonSerializationTest {

    @Test
    public void testEcho() {
        RestAssured
                .with()
                .body("{\"publicName\":\"Leo\",\"veterinarian\":{\"name\":\"Dolittle\"},\"age\":5}")
                .contentType("application/json; charset=utf-8")
                .post("/json-serialization/dog-echo")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("publicName", Matchers.is("Leo"))
                .body("privateName", Matchers.nullValue())
                .body("age", Matchers.is(5))
                .body("veterinarian.name", Matchers.is("Dolittle"))
                .body("veterinarian.title", Matchers.nullValue());
    }

    @Test
    public void testInterface() {
        RestAssured
                .with()
                .get("/json-serialization/interface")
                .then()
                .statusCode(200)
                .body("nestedInterface.int", Matchers.is(42))
                .body("nestedInterface.character", Matchers.is("a"))
                .body("nestedInterface.string", Matchers.is("response"));
    }
}
