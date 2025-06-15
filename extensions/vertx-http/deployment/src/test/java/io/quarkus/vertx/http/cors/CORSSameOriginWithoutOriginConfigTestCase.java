package io.quarkus.vertx.http.cors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class CORSSameOriginWithoutOriginConfigTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(BeanRegisteringRoute.class)
                    .addAsResource("conf/cors-same-origin-only.properties", "application.properties"));

    @Test
    void corsSameOriginRequest() {
        String origin = "http://localhost:8081";
        given().header("Origin", origin).get("/test").then().statusCode(200).header("Access-Control-Allow-Origin",
                origin);
    }

    @Test
    void corsInvalidSameOriginRequest() {
        String origin = "http://externalhost:8081";
        given().header("Origin", origin).get("/test").then().statusCode(403).header("Access-Control-Allow-Origin",
                nullValue());
    }
}
