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

public class ManagementHostValidationAllowedLocalHostTest {

    private static final String configuration = """
            quarkus.management.enabled=true
            quarkus.management.root-path=/management
            quarkus.management.host-validation.require-localhost=true
            """;

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(configuration), "application.properties")
                    .addClasses(MyObserver.class));

    @TestHTTPResource(value = "/my-route", management = true)
    URL myRoute;

    @Test
    void localHostRequest() {
        given().header("Host", "localhost:9001")
                .get(myRoute).then()
                .statusCode(200)
                .body(equalTo("test route"));
    }

    @Test
    void localHostCaseInsensitiveRequest() {
        given().header("Host", "LocalHost:9001")
                .get(myRoute).then()
                .statusCode(200)
                .body(equalTo("test route"));
    }

    @Test
    void localHostloopbackRequest() {
        given().header("Host", "127.0.0.1:9001")
                .get(myRoute).then()
                .statusCode(200)
                .body(equalTo("test route"));
    }

    @Test
    void publicHostRequest() {
        given().header("Host", "public-api.com:9001")
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
