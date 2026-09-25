package io.quarkus.swaggerui.deployment;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class ReverseProxyPathTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.http.non-application-root-path", "/");

    @Test
    public void shouldUseRelativeOpenApiUrl() {
        RestAssured.when().get("/swagger-ui").then().statusCode(200).body(containsString("../openapi"));
        RestAssured.when().get("/swagger-ui/index.html").then().statusCode(200).body(containsString("../openapi"));
    }
}
