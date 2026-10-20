package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that endpoints configured with trailing slashes (like "/static/")
 * are normalized correctly and don't cause double slashes in file paths.
 */
public class HttpStaticDirTrailingSlashEndpointTest {

    private static final String PUBLIC_RESOURCES_DIR = "src/test/resources/public-resources";

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.http.static-dir.enabled", "true")
            // Endpoint configured with trailing slash - should be normalized to "/static"
            .overrideConfigKey("quarkus.http.static-dir.endpoint", "/static/")
            .overrideConfigKey("quarkus.http.static-dir.path", PUBLIC_RESOURCES_DIR);

    @Test
    public void shouldNormalizeTrailingSlashAndServeCorrectly() {
        // Even though endpoint is configured as "/static/", normalizedEndpoint() removes
        // the trailing slash, so files should be served at /static/file.txt
        given()
                .when().get("/static/test.txt")
                .then()
                .statusCode(200)
                .body(equalTo("hello-static"));
    }

    @Test
    public void shouldNotHaveDoubleSlashWithTrailingSlashEndpoint() {
        // Files should NOT be accessible with double slashes
        given()
                .when().get("/static//test.txt")
                .then()
                .statusCode(404);
    }

    @Test
    public void shouldServeSubdirectoryWithTrailingSlashEndpoint() {
        given()
                .when().get("/static/subdir/test.txt")
                .then()
                .statusCode(200);
    }
}
