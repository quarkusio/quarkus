package io.quarkus.swaggerui.deployment;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class NoConfigTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication();

    @Test
    public void shouldUseDefaultConfig() {
        RestAssured.when().get("/q/swagger-ui").then().statusCode(200).body(containsString("/openapi"));
        RestAssured.when().get("/q/swagger-ui/index.html").then().statusCode(200).body(containsString("/openapi"));

    }
}
