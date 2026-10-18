package io.quarkus.vertx.http.cors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class CORSVaryOriginRejectionTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanRegisteringRoute.class)
                    .addAsResource("conf/cors-config-wildcard-origins.properties", "application.properties"));

    @Test
    @DisplayName("Vary: Origin is present when origin is rejected")
    public void corsVaryPresentOnRejection() {
        String origin = "http://non.matching.origin.quarkus";
        given().header("Origin", origin)
                .header("Access-Control-Request-Method", "POST")
                .when()
                .options("/test").then()
                .statusCode(403)
                .header("Access-Control-Allow-Origin", nullValue())
                .header("Vary", containsString("origin"));
    }
}
