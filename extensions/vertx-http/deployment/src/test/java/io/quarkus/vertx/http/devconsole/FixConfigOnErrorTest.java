package io.quarkus.vertx.http.devconsole;

import static org.hamcrest.Matchers.containsString;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class FixConfigOnErrorTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .setAllowFailedStart(true)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(ConfigBean.class));

    public void testFailedStartup() {
        RestAssured.with()
                .get("/msg")
                .then()
                .statusCode(500)
                .body(containsString("name=\"key.message\""));

        RestAssured.with()
                .redirects().follow(false)
                .formParam("key.message", "A Message")
                .formParam("redirect", "/")
                .post("/io.quarkus.vertx-http.devmode.config.fix")
                .then().statusCode(303);

        RestAssured.with()
                .get("/msg")
                .then()
                .statusCode(200)
                .body(containsString("A Message"));
    }

}
