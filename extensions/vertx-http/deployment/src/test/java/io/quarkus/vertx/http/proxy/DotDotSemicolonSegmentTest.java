package io.quarkus.vertx.http.proxy;

import static io.restassured.RestAssured.given;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Make sure that `/..;/` appended to a path is not resolved to `/`, as this
 * would allow to escape the allowed context when passing through proxies
 * (httpd does not recognize it as a double-dot segment and lets the request
 * through without sanitizing the path).
 */
public class DotDotSemicolonSegmentTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot(jar -> {
        jar.addAsResource(new StringAsset("Hello"), "META-INF/resources/index.html");
    });

    @Test
    public void testPathIsNotResolved() {
        given()
                .get("/index.html")
                .then()
                .statusCode(200);
        given()
                .get("/something/../index.html")
                .then()
                .statusCode(200);
        given()
                .get("/something/..;/index.html")
                .then()
                .statusCode(404);
    }

}
