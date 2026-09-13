package io.quarkus.vertx.http;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

/**
 * A request for a static directory without the trailing slash, including the bare root path, is redirected to the
 * slash-terminated form when the directory has an index page, in every run mode.
 */
public class StaticResourcesRootPathRedirectTest {

    @RegisterExtension
    final static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .add(new StringAsset("quarkus.http.root-path=/testing\n"), "application.properties")
                    .addAsResource("static-file.html", "META-INF/resources/index.html")
                    .addAsResource("static-file.html", "META-INF/resources/sub/index.html")
                    .addAsResource("static-file.html", "META-INF/resources/nested/dir/index.html"));

    @Test
    public void bareRootPathIsRedirectedToTheIndexPage() {
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
    public void staticDirectoryWithoutTrailingSlashIsRedirected() {
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
    public void staticDirectoryWithTrailingSlashServesTheIndexPage() {
        absolute().get("/testing/sub/")
                .then()
                .statusCode(200);
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
