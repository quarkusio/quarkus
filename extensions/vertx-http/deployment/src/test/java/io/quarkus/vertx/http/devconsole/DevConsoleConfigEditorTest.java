package io.quarkus.vertx.http.devconsole;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class DevConsoleConfigEditorTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Test
    public void testChangeHttpRoute() {
        RestAssured.with()
                .get("q/arc/beans")
                .then()
                .statusCode(200);
        RestAssured.with().formParam("name", "quarkus.http.root-path")
                .formParam("value", "/foo")
                .formParam("action", "updateProperty")
                .redirects().follow(false)
                .post("q/dev/io.quarkus.quarkus-vertx-http/config")
                .then()
                .statusCode(303);
        RestAssured.with()
                .get("q/arc/beans")
                .then()
                .statusCode(404);
        RestAssured.with()
                .get("foo/q/arc/beans")
                .then()
                .statusCode(200);

    }

    @Test
    public void testSetEmptyValue() {
        RestAssured.with()
                .get("q/arc/beans")
                .then()
                .statusCode(200);
        RestAssured.with().formParam("name", "quarkus.http.root-path")
                .formParam("value", "")
                .formParam("action", "updateProperty")
                .redirects().follow(false)
                .post("q/dev/io.quarkus.quarkus-vertx-http/config")
                .then()
                .statusCode(303);
        RestAssured.with()
                .get("q/arc/beans")
                .then()
                .statusCode(200);
    }
}
