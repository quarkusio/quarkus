package io.quarkus.vertx.http.devconsole;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;

public class DevConsoleConfigEditorTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withEmptyApplication();

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

    @Test
    public void testSetEmptyValue() {
        RestAssured.with()
                .get("http://localhost:" + url.getPort() + "/q/arc/beans")
                .then()
                .statusCode(200);
        RestAssured.with().formParam("name", "quarkus.http.root-path")
                .formParam("value", "")
                .formParam("action", "updateProperty")
                .redirects().follow(false)
                .post("http://localhost:" + url.getPort() + "/q/dev/io.quarkus.quarkus-vertx-http/config")
                .then()
                .statusCode(303);
        RestAssured.with()
                .get("http://localhost:" + url.getPort() + "/q/arc/beans")
                .then()
                .statusCode(200);
    }
}
