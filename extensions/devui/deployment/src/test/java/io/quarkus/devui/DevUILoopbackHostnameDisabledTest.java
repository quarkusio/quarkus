package io.quarkus.devui;

import static org.hamcrest.Matchers.emptyOrNullString;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * By default a host name that resolves to a loopback address is still rejected as the CORS origin.
 */
public class DevUILoopbackHostnameDisabledTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withEmptyApplication();

    @Test
    public void testPreflightFromLoopbackHostnameIsRejected() {
        String hostname = LoopbackHostnames.find();
        RestAssured.given()
                .header("Origin", "http://" + hostname + ":8080")
                .header("Access-Control-Request-Method", "GET,POST")
                .when()
                .options("q/dev-ui/configuration-form-editor").then()
                .statusCode(403)
                .body(emptyOrNullString());
    }
}
