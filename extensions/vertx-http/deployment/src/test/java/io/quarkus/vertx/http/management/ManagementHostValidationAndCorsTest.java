package io.quarkus.vertx.http.management;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;

import java.net.URL;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.ManagementInterface;

public class ManagementHostValidationAndCorsTest {

    private static final String configuration = """
            quarkus.management.enabled=true
            quarkus.management.root-path=/management
            quarkus.management.host-validation.allowed-hosts=localhost
            quarkus.management.cors.enabled=true
            """;

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(configuration), "application.properties")
                    .addClasses(MyObserver.class));

    @TestHTTPResource(value = "/my-route", management = true)
    URL myRoute;

    @Test
    void hostWithoutPortAndOrigin() {
        given().header("Host", "localhost")
                .header("Origin", "http://localhost:9001")
                .get(myRoute).then()
                .statusCode(403)
                .body(emptyString());
    }

    @Test
    void hostWithWrongPortAndOrigin() {
        given().header("Host", "localhost:9000")
                .header("Origin", "http://localhost:9001")
                .get(myRoute).then()
                .statusCode(403)
                .body(emptyString());
    }

    @Test
    void hostWithCorrectPortAndOrigin() {
        given().header("Host", "localhost:9001")
                .header("Origin", "http://localhost:9001")
                .get(myRoute).then()
                .statusCode(200)
                .body(equalTo("test route"));
    }

    @Test
    void wrongHostWithCorrectPortAndOrigin() {
        // With the host validation, the same origin check gives 200
        given().header("Host", "public-api.com:9001")
                .header("Origin", "http://public-api.com:9001")
                .get(myRoute).then()
                .statusCode(403)
                .body(emptyString());
    }

    @Singleton
    static class MyObserver {

        public void registerManagementRoutes(@Observes ManagementInterface mi) {
            mi.router().get("/management/my-route").handler(rc -> rc.response().end("test route"));
        }

    }
}
