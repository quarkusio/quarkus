package io.quarkus.reactive.mysql.client;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class ReactiveMySQLReloadTest {

    @RegisterExtension
    public static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DevModeResource.class)
                    .add(new StringAsset("quarkus.datasource.db-kind=mysql\n" +
                            "quarkus.datasource.reactive.url=vertx-reactive:mysql://localhost:6033/reload_test"),
                            "application.properties"));

    @Test
    public void testHotReplacement() {
        RestAssured
                .get("/dev/error")
                .then()
                .statusCode(200)
                .body(Matchers.endsWith(":6033"));

        test.modifyResourceFile("application.properties", s -> s.replace(":6033", ":9366"));

        RestAssured
                .get("/dev/error")
                .then()
                .statusCode(200)
                .body(Matchers.endsWith(":9366"));
    }
}
