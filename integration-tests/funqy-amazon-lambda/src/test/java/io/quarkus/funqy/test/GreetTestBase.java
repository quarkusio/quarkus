package io.quarkus.funqy.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.jupiter.api.Test;

public abstract class GreetTestBase {
    @Test
    public void testGreet() {
        Identity identity = new Identity();
        identity.setName("Matej");
        given()
                .contentType("application/json")
                .accept("application/json")
                .body(identity)
                .when()
                .post()
                .then()
                .statusCode(200)
                .body("name", equalTo("Matej"))
                .body("message", equalTo("Hello Matej!"));
    }

    @Test
    public void testGreetNPE() {
        given()
                .when()
                .post()
                .then()
                .statusCode(500)
                .body("errorMessage", containsString("end-of-input"));
    }

}
