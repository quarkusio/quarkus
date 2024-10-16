package io.quarkus.webdependency.locator.test;

import static org.hamcrest.core.Is.is;

import java.util.List;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class WebDependencyLocatorRootPathTest extends WebDependencyLocatorTestSupport {
    private static final String META_INF_RESOURCES = "META-INF/resources/";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("<html>Hello!<html>"), META_INF_RESOURCES + "index.html")
                    .addAsResource(new StringAsset("Test"), META_INF_RESOURCES + "some/path/test.txt"))
            .overrideConfigKey("quarkus.http.root-path", "/app")
            .setForcedDependencies(List.of(
                    Dependency.of("org.webjars", "jquery-ui", JQUERY_UI_VERSION),
                    Dependency.of("org.webjars", "momentjs", MOMENTJS_VERSION),
                    Dependency.of("org.mvnpm", "bootstrap", BOOTSTRAP_VERSION)));

    @Test
    public void test() {
        // Test normal files
        RestAssured.get("/").then()
                .statusCode(200)
                .body(is("<html>Hello!<html>"));
        RestAssured.get("/index.html").then()
                .statusCode(200)
                .body(is("<html>Hello!<html>"));

        RestAssured.get("/some/path/test.txt").then()
                .statusCode(200)
                .body(is("Test"));

        // Test Existing Web Jars
        RestAssured.get("/webjars/jquery-ui/jquery-ui.min.js").then()
                .statusCode(200);
        RestAssured.get("/webjars/momentjs/min/moment.min.js").then()
                .statusCode(200);
        RestAssured.get("/_static/bootstrap/dist/js/bootstrap.min.js").then()
                .statusCode(200);

        // Test using version in url of existing Web Jar
        RestAssured.get("/webjars/jquery-ui/" + JQUERY_UI_VERSION + "/jquery-ui.min.js").then()
                .statusCode(200);
        RestAssured.get("/webjars/momentjs/" + MOMENTJS_VERSION + "/min/moment.min.js").then()
                .statusCode(200);
        RestAssured.get("/_static/bootstrap/" + BOOTSTRAP_VERSION + "/dist/js/bootstrap.min.js").then()
                .statusCode(200);

        // Test non-existing Web Jar
        RestAssured.get("/webjars/bootstrap/js/bootstrap.min.js").then()
                .statusCode(404);
        RestAssured.get("/webjars/bootstrap/4.3.1/js/bootstrap.min.js").then()
                .statusCode(404);
        RestAssured.get("/webjars/momentjs/2.25.0/min/moment.min.js").then()
                .statusCode(404);
        RestAssured.get("/_static/foundation-sites/6.8.1/dist/js/foundation.esm.js").then()
                .statusCode(404);

        // Test webjar that does not have a version in the jar path
        RestAssured.get("/webjars/dcjs/dc.min.js").then()
                .statusCode(200);
    }
}
