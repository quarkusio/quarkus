package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class HttpStaticDirRootEndpointTest {

    private static final String PUBLIC_RESOURCES_DIR = "src/test/resources/public-resources";

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.http.static-dir.enabled", "true")
            .overrideConfigKey("quarkus.http.static-dir.endpoint", "/")
            .overrideConfigKey("quarkus.http.static-dir.path", PUBLIC_RESOURCES_DIR);

    @Test
    public void shouldServeStaticFilesAtRootEndpoint() {
        given()
                .when().get("/test.txt")
                .then()
                .statusCode(200)
                .body(equalTo("hello-static"));
    }

    @Test
    public void shouldServeSubdirectoryResourcesAtRootEndpoint() {
        given()
                .when().get("/subdir/test.txt")
                .then()
                .statusCode(200);
    }

    @Test
    public void shouldReturn404ForMissingResourceAtRootEndpoint() {
        given()
                .when().get("/does-not-exist.txt")
                .then()
                .statusCode(404);
    }

    @Test
    public void shouldNotServeResourcesWithDoubleSlash() {
        given()
                .when().get("//test.txt")
                .then()
                .statusCode(404);
    }

    @Test
    public void shouldHandlePathNormalization() {
        given()
                .when().get("/../test.txt")
                .then()
                .statusCode(200)
                .body(equalTo("hello-static"));

        given()
                .when().get("/../../etc/passwd")
                .then()
                .statusCode(404);

        given()
                .when().get("/%2e%2e/test.txt")
                .then()
                .statusCode(404);
    }
}
