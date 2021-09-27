package io.quarkus.amazon.lambda.deployment.testing;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class LambdaHandlerET {

    @Test
    public void testSimpleLambdaSuccess() throws Exception {
        // you test your lambas by invoking on http://localhost:8081
        // this works in dev mode too

        Person in = new Person();
        in.setName("Stu");
        given()
                .contentType("application/json")
                .accept("application/json")
                .body(in)
                .when()
                .post()
                .then()
                .statusCode(200)
                .body(containsString("Hey Stu"));
    }

}
