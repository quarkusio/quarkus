package io.quarkus.swaggerui.deployment;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class DisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.swagger-ui.enabled=false"), "application.properties"));

    @Test
    public void shouldUseDefaultConfig() {
        RestAssured.when().get("/q/swagger-ui").then().statusCode(404);
        RestAssured.when().get("/q/swagger-ui/index.html").then().statusCode(404);
    }
}
