package io.quarkus.smallrye.health.test.ui;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

class DisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.smallrye-health.ui.enable=false"), "application.properties"));

    @Test
    void shouldUseDefaultConfig() {
        RestAssured.when().get("/q/health-ui").then().statusCode(404);
    }
}
