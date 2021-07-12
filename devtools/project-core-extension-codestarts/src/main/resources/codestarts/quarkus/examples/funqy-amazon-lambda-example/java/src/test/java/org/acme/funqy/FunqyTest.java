package org.acme.funqy;

import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
public class FunqyTest {

    @Test
    public void testFunqyLambda() throws Exception {
        // you test your lambas by invoking on http://localhost:8081
        // this works in dev mode too

        Person in = new Person();
        in.setName("Bill");
        given()
                .contentType("application/json")
                .accept("application/json")
                .body(in)
                .when()
                .post()
                .then()
                .statusCode(200)
                .body(containsString("Hello Bill"));
    }

}

