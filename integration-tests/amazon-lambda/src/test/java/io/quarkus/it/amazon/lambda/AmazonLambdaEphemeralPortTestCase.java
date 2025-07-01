package io.quarkus.it.amazon.lambda;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.it.amazon.lambda.profiles.EphemeralPortProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@Disabled("Failing since 3.22, should be fixed by by https://github.com/quarkusio/quarkus/issues/48006")
@TestProfile(EphemeralPortProfile.class)
@QuarkusTest
public class AmazonLambdaEphemeralPortTestCase {

    @Test
    public void testSimpleLambdaSuccess() {
        InputObject in = new InputObject();
        in.setGreeting("Hello");
        in.setName("Stu");
        given()
                .contentType("application/json")
                .accept("application/json")
                .body(in)
                .when()
                .post()
                .then()
                .statusCode(200)
                .body(containsString("Hello Stu"));
    }
}
