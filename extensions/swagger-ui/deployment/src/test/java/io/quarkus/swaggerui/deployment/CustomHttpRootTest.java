package io.quarkus.swaggerui.deployment;

import static org.hamcrest.Matchers.containsString;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class CustomHttpRootTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.http.root-path=/foo"), "application.properties"));

    @Test
    public void shouldUseCustomConfig() {
        RestAssured.when().get("/q/swagger-ui").then().statusCode(200).body(containsString("/q/openapi"));
        RestAssured.when().get("/q/swagger-ui/index.html").then().statusCode(200).body(containsString("/q/openapi"));
    }
}
