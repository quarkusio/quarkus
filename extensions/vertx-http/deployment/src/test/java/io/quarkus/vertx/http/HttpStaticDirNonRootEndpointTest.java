package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that the double-slash fix for root endpoint ("/") doesn't break
 * non-root endpoints like "/static".
 */
public class HttpStaticDirNonRootEndpointTest {

    private static final String PUBLIC_RESOURCES_DIR = "src/test/resources/public-resources";

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.http.static-dir.enabled", "true")
            .overrideConfigKey("quarkus.http.static-dir.endpoint", "/static")
            .overrideConfigKey("quarkus.http.static-dir.path", PUBLIC_RESOURCES_DIR);

    @Test
    public void shouldServeFilesAtNonRootEndpoint() {
        // Verify files are served at /static/file.txt (not /static//file.txt)
        given()
                .when().get("/static/test.txt")
                .then()
                .statusCode(200)
                .body(equalTo("hello-static"));
    }

    @Test
    public void shouldServeSubdirectoryFilesAtNonRootEndpoint() {
        given()
                .when().get("/static/subdir/test.txt")
                .then()
                .statusCode(200);
    }

    @Test
    public void shouldReturn404ForDoubleSlashAtNonRootEndpoint() {
        // Files should not be accessible with double slashes
        given()
                .when().get("/static//test.txt")
                .then()
                .statusCode(404);
    }

    @Test
    public void shouldReturn404WithoutEndpointPrefix() {
        // Files should not be accessible without the /static prefix
        given()
                .when().get("/test.txt")
                .then()
                .statusCode(404);
    }
}
