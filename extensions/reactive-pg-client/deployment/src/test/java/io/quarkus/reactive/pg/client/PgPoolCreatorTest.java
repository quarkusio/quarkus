package io.quarkus.reactive.pg.client;

import static io.restassured.RestAssured.given;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class PgPoolCreatorTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(CustomCredentialsProvider.class)
                    .addClass(CredentialsTestResource.class)
                    .addClass(LocalhostPgPoolCreator.class)
                    .addAsResource("application-credentials-with-erroneous-url.properties", "application.properties"));

    @Test
    public void testConnect() {
        given()
                .when().get("/test")
                .then()
                .statusCode(200)
                .body(CoreMatchers.equalTo("OK"));
    }

}
