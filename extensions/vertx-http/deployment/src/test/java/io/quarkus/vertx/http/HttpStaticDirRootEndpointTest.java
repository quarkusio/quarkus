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

    @Test
    public void shouldNotHaveDoubleSlashInRegisteredPaths() {
        // This test verifies the fix for issue #57153
        // When endpoint="/", files must be registered at "/file.txt" not "//file.txt"
        // The fix ensures basePath.endsWith("/") check prevents double slashes

        // Test 1: Root-level file should be accessible at /test.txt (not //test.txt)
        given()
                .when().get("/test.txt")
                .then()
                .statusCode(200)
                .body(equalTo("hello-static"));

        // Test 2: Subdirectory file should be accessible at /subdir/test.txt (not //subdir/test.txt)
        given()
                .when().get("/subdir/test.txt")
                .then()
                .statusCode(200);

        // Test 3: Files should NOT be accessible with double slashes
        // (This verifies files are registered correctly, not at wrong paths)
        given()
                .when().get("//test.txt")
                .then()
                .statusCode(404);

        given()
                .when().get("//subdir/test.txt")
                .then()
                .statusCode(404);

        // If the bug regresses (files registered at //test.txt instead of /test.txt),
        // tests 1-2 would fail with 404 because files wouldn't be found at their
        // expected single-slash paths
    }
}
