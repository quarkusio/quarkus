package io.quarkus.test.qute;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.containsString;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class QuteWatchedResourceTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot(
                    root -> root.addClass(HelloResource.class)
                            .addAsResource(new StringAsset("Hello {name}!"), "templates/hello.txt"));

    @Test
    public void testWatchedFiles() {
        when().get("/hello?name=Martin").then()
                .body(containsString("Hello Martin!"))
                .statusCode(200);

        config.modifyResourceFile("templates/hello.txt", file -> "Hi {name}!");

        when().get("/hello?name=Martin").then()
                .body(containsString("Hi Martin!"))
                .statusCode(200);

        config.modifyResourceFile("templates/hello.txt", file -> "Hello {name}!");

        when().get("/hello?name=Martin").then()
                .body(containsString("Hello Martin!"))
                .statusCode(200);

        config.addResourceFile("templates/ping.txt", "pong");

        when().get("/hello/ping").then()
                .body(containsString("pong"))
                .statusCode(200);

        config.modifyResourceFile("templates/ping.txt", file -> "pong!");

        when().get("/hello/ping").then()
                .body(containsString("pong!"))
                .statusCode(200);
    }

}
