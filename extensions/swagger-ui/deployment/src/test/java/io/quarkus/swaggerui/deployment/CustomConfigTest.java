package io.quarkus.swaggerui.deployment;

import static org.hamcrest.Matchers.containsString;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class CustomConfigTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.swagger-ui.path=/custom"), "application.properties"));

    @Test
    public void shouldUseCustomConfig() {
        RestAssured.when().get("/custom").then().statusCode(200).body(containsString("/openapi"));
        RestAssured.when().get("/custom/index.html").then().statusCode(200).body(containsString("/openapi"));
    }
}
