package io.quarkus.reactive.mssql.client;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class ReactiveMSSQLReloadTest {

    @RegisterExtension
    public static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DevModeResource.class)
                    .add(new StringAsset("quarkus.datasource.db-kind=mssql\n" +
                            "quarkus.datasource.reactive.url=vertx-reactive:sqlserver://localhost:3314/reload_test"),
                            "application.properties"));

    @Test
    public void testHotReplacement() {
        RestAssured
                .get("/dev/error")
                .then()
                .statusCode(200)
                .body(Matchers.endsWith(":3314"));

        test.modifyResourceFile("application.properties", s -> s.replace(":3314", ":9314"));

        RestAssured
                .get("/dev/error")
                .then()
                .statusCode(200)
                .body(Matchers.endsWith(":9314"));
    }
}
