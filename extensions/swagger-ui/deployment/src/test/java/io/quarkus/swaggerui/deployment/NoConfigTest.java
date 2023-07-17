package io.quarkus.swaggerui.deployment;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class NoConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication();

    @Test
    public void shouldUseDefaultConfig() {
        RestAssured.when().get("/q/swagger-ui").then().statusCode(200).body(containsString("/openapi"));
        RestAssured.when().get("/q/swagger-ui/index.html").then().statusCode(200).body(containsString("/openapi"));

    }
}
