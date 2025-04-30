package io.quarkus.webdependency.locator.test;

import static org.hamcrest.Matchers.containsString;

import java.util.List;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ImportMapTest extends WebDependencyLocatorTestSupport {
    private static final String META_INF_RESOURCES = "META-INF/resources/";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("<html>Hello!<html>"), META_INF_RESOURCES + "/index.html")
                    .addAsResource(new StringAsset("Test"), META_INF_RESOURCES + "/some/path/test.txt"))
            .setForcedDependencies(List.of(
                    Dependency.of("org.mvnpm", "bootstrap", BOOTSTRAP_VERSION)));

    @Test
    public void test() {
        // Test normal files
        RestAssured.get("/_importmap/generated_importmap.js").then()
                .statusCode(200)
                .body(containsString("\"bootstrap/\" : \"/_static/bootstrap/" + BOOTSTRAP_VERSION + "/dist/\""));

    }
}
