package io.quarkus.it.webjar.locator;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class WebJarResourceTest {

    private static final String JQUERY_UI_VERSION = System.getProperty("webjar.jquery-ui.version");
    private static final String MOMENTJS_VERSION = System.getProperty("webjar.momentjs.version");

    @Test
    void testWebJar() {
        // Test Existing Web Jars
        RestAssured.get("/webjars/jquery/jquery.min.js").then()
                .statusCode(200);
        RestAssured.get("/webjars/momentjs/min/moment.min.js").then()
                .statusCode(200);

        // Test using version in url of existing Web Jar
        RestAssured.get("/webjars/jquery-ui/" + JQUERY_UI_VERSION + "/jquery-ui.min.js").then()
                .statusCode(200);
        RestAssured.get("/webjars/momentjs/" + MOMENTJS_VERSION + "/min/moment.min.js").then()
                .statusCode(200);

        // Test non-existing Web Jar
        RestAssured.get("/webjars/bootstrap/js/bootstrap.min.js").then()
                .statusCode(404);
        RestAssured.get("/webjars/bootstrap/4.3.1/js/bootstrap.min.js").then()
                .statusCode(404);
        RestAssured.get("/webjars/momentjs/2.25.0/min/moment.min.js").then()
                .statusCode(404);
    }
}
