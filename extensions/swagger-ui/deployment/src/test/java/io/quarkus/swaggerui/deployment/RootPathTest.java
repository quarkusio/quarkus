package io.quarkus.swaggerui.deployment;

import static org.hamcrest.Matchers.containsString;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class RootPathTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(
                            "quarkus.swagger-ui.root-path=/nginx/path"), "application.properties"));

    @Test
    public void shouldUseCustomRootPath() {
        RestAssured.when().get("/q/swagger-ui").then().statusCode(200).body(containsString("/nginx/path/q/openapi"));
        RestAssured.when().get("/q/swagger-ui/index.html").then().statusCode(200).body(containsString("/nginx/path/q/openapi"));
        RestAssured.when().get("/q/swagger-ui").then()
                .statusCode(200)
                .body(containsString("/nginx/path/q/swagger-ui/oauth2-redirect.html"));
        RestAssured.when().get("/q/swagger-ui/index.html").then()
                .statusCode(200)
                .body(containsString("/nginx/path/q/swagger-ui/oauth2-redirect.html"));
        RestAssured.when().get("/q/swagger-ui").then()
                .statusCode(200)
                .body(containsString("/nginx/path/q/swagger-ui"));
        RestAssured.when().get("/q/swagger-ui/index.html").then()
                .statusCode(200)
                .body(containsString("/nginx/path/q/swagger-ui"));
    }
}
