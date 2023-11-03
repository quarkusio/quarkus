package io.quarkus.reactive.mysql.client;

import static io.restassured.RestAssured.given;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class CredentialsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(CustomCredentialsProvider.class)
                    .addClass(CredentialsTestResource.class)
                    .addAsResource("application-credentials.properties", "application.properties"));

    @Test
    public void testConnect() {
        given()
                .when().get("/test")
                .then()
                .statusCode(200)
                .body(CoreMatchers.equalTo("OK"));
    }

}
