package io.quarkus.vertx.http.devconsole;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;

/**
 * This tests that the dev console still works with this extension present (i.e. with a RequireBodyHandlerBuildItem being
 * produced)
 * <p>
 * The config editor is a deployment side handler, so this test that the deployment side functions of the dev console
 * handle body handlers correctly.
 */
public class DevConsoleConfigEditorBodyHandlerTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addClass(BodyHandlerBean.class));

    @TestHTTPResource
    URL url;

    @Test
    public void testChangeHttpRoute() {
        RestAssured.with()
                .get("http://localhost:" + url.getPort() + "/q/arc/beans")
                .then()
                .statusCode(200);
        RestAssured.with().formParam("name", "quarkus.http.root-path")
                .formParam("value", "/foo")
                .formParam("action", "updateProperty")
                .redirects().follow(false)
                .post("http://localhost:" + url.getPort() + "/q/dev/io.quarkus.quarkus-vertx-http/config")
                .then()
                .statusCode(303);
        RestAssured.with()
                .get("http://localhost:" + url.getPort() + "/q/arc/beans")
                .then()
                .statusCode(404);
        RestAssured.with()
                .get("http://localhost:" + url.getPort() + "/foo/q/arc/beans")
                .then()
                .statusCode(200);

    }

}
