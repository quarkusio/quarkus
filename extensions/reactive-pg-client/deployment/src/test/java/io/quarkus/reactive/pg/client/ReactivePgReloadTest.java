package io.quarkus.reactive.pg.client;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class ReactivePgReloadTest {

    @RegisterExtension
    public static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DevModeResource.class)
                    .add(new StringAsset("quarkus.datasource.db-kind=postgresql\n" +
                            "quarkus.datasource.reactive.url=vertx-reactive:postgres://localhost:2345/reload_test"),
                            "application.properties"));

    @Test
    public void testHotReplacement() {
        RestAssured
                .get("/dev/error")
                .then()
                .statusCode(200)
                .body(Matchers.endsWith(":2345"));

        test.modifyResourceFile("application.properties", s -> s.replace(":2345", ":5234"));

        RestAssured
                .get("/dev/error")
                .then()
                .statusCode(200)
                .body(Matchers.endsWith(":5234"));
    }
}
