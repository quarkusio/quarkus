package io.quarkus.vertx.http.cors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.nullValue;

import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.security.CORS;
import io.quarkus.vertx.http.security.HttpSecurity;

public class CORSFluentApiSameOriginLocalHostTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanRegisteringRoute.class, CorsProgrammaticConfig.class));

    @Test
    void corsSameOriginRequest() {
        String origin = "http://localhost:8081";
        given().header("Origin", origin)
                .header("Host", "localhost:8081")
                .get("/test").then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", origin);
    }

    @Test
    void corsSameOriginRequestFails() {
        String origin = "http://localhost:8081";
        given().header("Origin", origin)
                .header("Host", "localhost:8080")
                .get("/test").then()
                .statusCode(403)
                .header("Access-Control-Allow-Origin", nullValue());
    }

    @Test
    void corsSameOriginPublicHostRequestFails() {
        String origin = "http://externalhost:8081";
        given().header("Origin", origin)
                .header("Host", "externalhost:8081")
                .get("/test").then()
                .statusCode(403)
                .header("Access-Control-Allow-Origin", nullValue());
    }

    public static class CorsProgrammaticConfig {
        void configure(@Observes HttpSecurity httpSecurity) {
            httpSecurity.cors(CORS.builder().origin("someorigin").sameOriginLocalHost(true).build());
        }
    }
}
