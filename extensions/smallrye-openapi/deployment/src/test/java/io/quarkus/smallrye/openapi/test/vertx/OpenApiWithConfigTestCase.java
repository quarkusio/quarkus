package io.quarkus.smallrye.openapi.test.vertx;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenApiWithConfigTestCase {
    private static final String OPEN_API_PATH = "/q/openapi";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiRoute.class)
                    .addAsManifestResource("test-openapi.yaml", "openapi.yaml")
                    .addAsResource(new StringAsset("mp.openapi.scan.disable=true\nmp.openapi.servers=https://api.acme.org/"),
                            "application.properties"));

    @Test
    public void testOpenAPI() {
        RestAssured.given().header("Accept", "application/json")
                .when().get(OPEN_API_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("openapi", Matchers.startsWith("3.0"))
                .body("info.title", Matchers.equalTo("Test OpenAPI"))
                .body("info.description", Matchers.equalTo("Some description"))
                .body("info.version", Matchers.equalTo("4.2"))
                .body("servers[0].url", Matchers.equalTo("https://api.acme.org/"))
                .body("paths", Matchers.hasKey("/openapi"))
                .body("paths", Matchers.not(Matchers.hasKey("/resource")));
    }
}
