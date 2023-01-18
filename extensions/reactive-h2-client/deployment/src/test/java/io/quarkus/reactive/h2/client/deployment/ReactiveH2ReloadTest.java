package io.quarkus.reactive.h2.client.deployment;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class ReactiveH2ReloadTest {

    @RegisterExtension
    public static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DevModeResource.class)
                    .addAsResource(new StringAsset("quarkus.datasource.db-kind=h2\n" +
                            "quarkus.datasource.reactive.url=vertx-reactive:h2:mem:reload_test"),
                            "application.properties"));

    @Test
    public void testHotReplacement() {
        RestAssured
                .get("/dev/dbname")
                .then()
                .statusCode(200)
                .body(Matchers.equalToIgnoringCase("reload_test"));

        test.modifyResourceFile("application.properties", s -> s.replace(":reload_test", ":test_reload"));

        RestAssured
                .get("/dev/dbname")
                .then()
                .statusCode(200)
                .body(Matchers.equalToIgnoringCase("test_reload"));
    }
}
