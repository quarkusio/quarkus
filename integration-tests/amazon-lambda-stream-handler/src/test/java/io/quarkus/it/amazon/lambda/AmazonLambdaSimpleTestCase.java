package io.quarkus.it.amazon.lambda;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AmazonLambdaSimpleTestCase {

    @Test
    public void testSimpleLambdaSuccess() throws Exception {
        given()
                .body("lowercase")
                .when()
                .post()
                .then()
                .statusCode(200)
                .body(equalTo("LOWERCASE"));
    }
}
