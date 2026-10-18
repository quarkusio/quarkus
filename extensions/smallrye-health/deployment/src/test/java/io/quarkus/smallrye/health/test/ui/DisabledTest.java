package io.quarkus.smallrye.health.test.ui;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

class DisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.smallrye-health.ui.enabled=false"), "application.properties"));

    @Test
    void shouldUseDefaultConfig() {
        RestAssured.when().get("/q/health-ui").then().statusCode(404);
    }
}
