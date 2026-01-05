package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class HttpStaticDirTest {

    private static final String PUBLIC_RESOURCES_DIR = "src/test/resources/public-resources";

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.http.static-dir.enabled", "true")
            .overrideConfigKey("quarkus.http.static-dir.endpoint", "/public-resources")
            .overrideConfigKey("quarkus.http.static-dir.path", PUBLIC_RESOURCES_DIR);

    @Test
    public void shouldServeHttpStaticDir() {
        given()
                .when().get("/public-resources/test.txt")
                .then()
                .statusCode(200)
                .body(equalTo("hello-static"));
    }

    @Test
    public void shouldReturn404ForMissingResource() {
        given()
                .when().get("/public-resources/does-not-exist.txt")
                .then()
                .statusCode(404);
    }

    @Test
    public void shouldServeSubdirectoryResources() {
        given()
                .when().get("/public-resources/subdir/test.txt")
                .then()
                .statusCode(200);
    }

    @Test
    public void shouldReturn404ForTraversal() {
        given()
                .when().get("/public-resources/../test.txt")
                .then()
                .statusCode(404);

        given()
                .when().get("/public-resources/../../etc/passwd")
                .then()
                .statusCode(404);

        given()
                .when().get("/public-resources/%2e%2e/test.txt")
                .then()
                .statusCode(404);

        // Returns 200 because Vert.x normalizes the path. The ../public-resources/ segment
        // collapses to the same directory, so the resolved path remains under /public-resources.
        given()
                .when().get("/public-resources/../public-resources/test.txt")
                .then()
                .statusCode(200);
    }
}