package io.quarkus.vertx.http.devui;

import static org.hamcrest.Matchers.emptyOrNullString;

import java.net.Inet4Address;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class DevUIRemoteCorsTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .setBuildSystemProperty("quarkus.http.host", "0.0.0.0")
            .withEmptyApplication();

    @Test
    public void test() throws UnknownHostException {
        String origin = Inet4Address.getLocalHost().toString();
        if (origin.contains("/")) {
            origin = "http://" + origin.split("/")[1] + ":8080";
        }
        String methods = "GET,POST";
        RestAssured.given()
                .header("Origin", origin)
                .header("Access-Control-Request-Method", methods)
                .when()
                .options("q/dev-ui/configuration-form-editor").then()
                .statusCode(403)
                .body(emptyOrNullString());
    }

}
