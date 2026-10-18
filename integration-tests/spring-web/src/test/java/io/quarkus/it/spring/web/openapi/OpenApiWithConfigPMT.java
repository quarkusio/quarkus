package io.quarkus.it.spring.web.openapi;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;
import io.restassured.RestAssured;

public class OpenApiWithConfigPMT {
    private static final String OPEN_API_PATH = "/q/openapi";

    @RegisterExtension
    static QuarkusProdModeTest runner = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiController.class)
                    .addAsResource("test-roles.properties")
                    .addAsResource("test-users.properties")
                    .addAsManifestResource("test-openapi.yaml", "openapi.yaml"))
            .overrideConfigKey("mp.openapi.scan.disable", "true")
            .overrideConfigKey("mp.openapi.servers", "https://api.acme.org/")
            .setRun(true);

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
