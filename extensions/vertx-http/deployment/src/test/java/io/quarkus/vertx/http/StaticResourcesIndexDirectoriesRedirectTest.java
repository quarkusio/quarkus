package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class StaticResourcesIndexDirectoriesRedirectTest extends AbstractStaticResourcesIndexDirectoriesTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = test("redirect");

    @Test
    public void directoryWithoutTrailingSlashIsRedirected() {
        given().config(NO_REDIRECTS).get("/classpath").then().statusCode(301).header("Location", endsWith("/classpath/"));
        given().config(NO_REDIRECTS).get("/generated").then().statusCode(301).header("Location", endsWith("/generated/"));
        given().config(NO_REDIRECTS).get("/classpath?q=1").then().statusCode(301)
                .header("Location", endsWith("/classpath/?q=1"));
    }
}
