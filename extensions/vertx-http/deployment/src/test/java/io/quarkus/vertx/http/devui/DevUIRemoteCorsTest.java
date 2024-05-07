package io.quarkus.vertx.http.devui;

import static org.hamcrest.Matchers.emptyOrNullString;

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
        String methods = "GET,POST";
        RestAssured.given()
                .header("Origin", "http://evilsite.com")
                .header("Access-Control-Request-Method", methods)
                .when()
                .options("q/dev-ui/configuration-form-editor").then()
                .statusCode(403)
                .body(emptyOrNullString());
    }

}
