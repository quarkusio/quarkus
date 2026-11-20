package io.quarkus.vertx.http;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

public class StaticResourcesIndexDirectoriesRootPathTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.http.root-path", "/testing")
            .overrideConfigKey("quarkus.http.static-resources.index-directories", "redirect")
            .withApplicationRoot((jar) -> jar
                    .addAsResource("static-file.html", "META-INF/resources/index.html")
                    .addAsResource("static-file.html", "META-INF/resources/sub/index.html")
                    .addAsResource("static-file.html", "META-INF/resources/nested/dir/index.html"));

    @Test
    public void bareRootPathIsRedirected() {
        absolute().get("/testing")
                .then()
                .statusCode(301)
                .header("Location", "/testing/");
    }

    @Test
    public void queryStringIsKeptByTheRedirect() {
        absolute().get("/testing?x=1&y=2")
                .then()
                .statusCode(301)
                .header("Location", "/testing/?x=1&y=2");
    }

    @Test
    public void rootPathWithTrailingSlashServesTheIndexPage() {
        absolute().get("/testing/")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("This is the title of the webpage!"));
    }

    @Test
    public void directoryUnderTheRootPathIsRedirected() {
        absolute().get("/testing/sub")
                .then()
                .statusCode(301)
                .header("Location", "/testing/sub/");
        absolute().get("/testing/nested/dir")
                .then()
                .statusCode(301)
                .header("Location", "/testing/nested/dir/");
    }

    @Test
    public void unknownPathIsStillNotFound() {
        absolute().get("/testing/unknown")
                .then()
                .statusCode(404);
        absolute().get("/testing/nested")
                .then()
                .statusCode(404);
    }

    private static RequestSpecification absolute() {
        return RestAssured.given().basePath("").redirects().follow(false);
    }
}
