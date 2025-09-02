package io.quarkus.vertx.http.cors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.time.Duration;
import java.util.Set;

import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.security.CORS;
import io.quarkus.vertx.http.security.HttpSecurity;

public class CORSFluentApiFullConfigHandlerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanRegisteringRoute.class, CorsProgrammaticConfig.class));

    @Test
    @DisplayName("Handles a detailed CORS config request correctly")
    public void corsFullConfigTestServlet() {
        given().header("Origin", "http://custom.origin.quarkus")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "X-Custom")
                .when()
                .options("/test").then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", "http://custom.origin.quarkus")
                .header("Access-Control-Allow-Methods", containsString("GET"))
                .header("Access-Control-Allow-Methods", containsString("PUT"))
                .header("Access-Control-Allow-Methods", containsString("POST"))
                .header("Access-Control-Allow-Methods", not(containsString("DELETE")))
                .header("Access-Control-Expose-Headers", "Content-Disposition")
                .header("Access-Control-Allow-Headers", "X-Custom")
                .header("Access-Control-Allow-Credentials", "false")
                .header("Access-Control-Max-Age", "86400");

        given().header("Origin", "http://www.quarkus.io")
                .header("Access-Control-Request-Method", "PUT")
                .when()
                .options("/test").then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", "http://www.quarkus.io")
                .header("Access-Control-Allow-Methods", containsString("PUT"))
                .header("Access-Control-Allow-Methods", containsString("GET"))
                .header("Access-Control-Allow-Methods", containsString("POST"))
                .header("Access-Control-Allow-Methods", not(containsString("DELETE")))
                .header("Access-Control-Allow-Credentials", "false")
                .header("Access-Control-Expose-Headers", "Content-Disposition");
    }

    @Test
    @DisplayName("Returns only allowed headers and methods")
    public void corsPartialMethodsTestServlet() {
        given().header("Origin", "http://custom.origin.quarkus")
                .header("Access-Control-Request-Method", "DELETE")
                .header("Access-Control-Request-Headers", "X-Custom, X-Custom2")
                .when()
                .options("/test").then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", "http://custom.origin.quarkus")
                .header("Access-Control-Allow-Methods", containsString("GET"))
                .header("Access-Control-Allow-Methods", containsString("PUT"))
                .header("Access-Control-Allow-Methods", containsString("POST"))
                .header("Access-Control-Allow-Methods", not(containsString("DELETE")))
                .header("Access-Control-Expose-Headers", "Content-Disposition")
                .header("Access-Control-Allow-Credentials", "false")
                .header("Access-Control-Allow-Headers", "X-Custom");// Should not return X-Custom2
    }

    public static class CorsProgrammaticConfig {
        void configure(@Observes HttpSecurity httpSecurity) {
            httpSecurity.cors(CORS.builder()
                    .origins(Set.of("http://custom.origin.quarkus", "http://www.quarkus.io"))
                    .methods(Set.of("GET", "PUT", "POST"))
                    .header("X-Custom")
                    .exposedHeader("Content-Disposition")
                    .accessControlMaxAge(Duration.ofDays(1))
                    .accessControlAllowCredentials(false)
                    .build());
        }
    }
}
