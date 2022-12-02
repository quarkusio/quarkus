package io.quarkus.swaggerui.deployment;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class DisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.swagger-ui.enable=false"), "application.properties"));

    @Test
    public void shouldUseDefaultConfig() {
        RestAssured.when().get("/q/swagger-ui").then().statusCode(404);
        RestAssured.when().get("/q/swagger-ui/index.html").then().statusCode(404);
    }
}
