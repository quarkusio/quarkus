package io.quarkus.vertx.graphql.deployment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class ServingUIFromDefaultPathTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication();

    @Test
    public void shouldServeVertxGraphqlUiFromDefaultPath() {
        RestAssured.when().get("/q/graphql-ui").then().statusCode(200);
    }
}
