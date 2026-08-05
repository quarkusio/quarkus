package io.quarkus.vertx.graphql.deployment;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class ServingUIFromCustomPathTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.vertx-graphql.ui.path=/custom\n"), "application.properties"));

    @Test
    public void shouldServeVertxGraphqlUiFromCustomPath() {
        RestAssured.when().get("/custom").then().statusCode(200);
    }
}
