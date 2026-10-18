package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class MyFunctionsTest {
    @Test
    void testFun() {
        given()
            .post("/fun")
            .then()
            .statusCode(200)
            .body(containsString("Hello Funqy!"));
    }

    @Test
    void testFunWithName() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"name\": \"Friend\"}")
            .post("/fun")
            .then()
            .statusCode(200)
            .body(containsString("Hello Friend!"));
    }

}
