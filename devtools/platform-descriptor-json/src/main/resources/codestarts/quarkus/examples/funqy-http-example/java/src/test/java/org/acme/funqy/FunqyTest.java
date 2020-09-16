package org.acme.funqy;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
public class FunqyTest {

    @Test
    public void testGreeting() {
        given()
                .contentType("application/json")
                .body("{\"name\": \"Bill\"}")
                .post("/myFunqyGreeting")
                .then()
                .statusCode(200)
                .body(equalTo("\"Hello Bill\""));
    }

}
