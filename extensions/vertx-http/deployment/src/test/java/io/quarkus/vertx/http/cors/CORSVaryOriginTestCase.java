package io.quarkus.vertx.http.cors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class CORSVaryOriginTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanRegisteringRoute.class)
                    .addAsResource("conf/cors-config.properties", "application.properties"));

    @Test
    @DisplayName("Vary: Origin is present when echoing exact origin on simple request")
    public void corsVaryPresentOnSimpleRequest() {
        String origin = "http://custom.origin.quarkus";
        given().header("Origin", origin)
                .when()
                .get("/test").then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", origin)
                .header("Vary", containsString("origin"));
    }

    @Test
    @DisplayName("Vary: Origin is present when echoing exact origin on preflight request")
    public void corsVaryPresentOnPreflightRequest() {
        String origin = "http://custom.origin.quarkus";
        given().header("Origin", origin)
                .header("Access-Control-Request-Method", "POST")
                .when()
                .options("/test").then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", origin)
                .header("Vary", containsString("origin"));
    }
}
