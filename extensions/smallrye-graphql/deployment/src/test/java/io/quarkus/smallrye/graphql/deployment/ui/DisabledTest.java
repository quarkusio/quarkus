package io.quarkus.smallrye.graphql.deployment.ui;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class DisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.smallrye-graphql.ui.enabled=false"), "application.properties"));

    @Test
    public void shouldUseDefaultConfig() {
        RestAssured.when().get("/graphql-ui").then().statusCode(404);
    }
}
