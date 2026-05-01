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

public class ManagementHostValidationWithForwardedHostTest {

    private static final String configuration = """
            quarkus.management.enabled=true
            quarkus.management.root-path=/management
            quarkus.management.proxy.proxy-address-forwarding=true
            quarkus.management.proxy.allow-forwarded=true
            quarkus.management.host-validation.allowed-hosts=public-api.com
            """;

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(configuration), "application.properties")
                    .addClasses(MyObserver.class));

    @TestHTTPResource(value = "/my-route", management = true)
    URL myRoute;

    @Test
    void somePublicApiHostRequest() {
        given().header("Host", "some-public-api")
                .get(myRoute).then()
                .statusCode(403)
                .body(emptyString());
    }

    @Test
    void somePublicApiHostWithForwardedHostRequest() {
        given().header("Host", "some-public-api")
                .header("Forwarded", "host=public-api.com")
                .get(myRoute).then()
                .statusCode(200)
                .body(equalTo("test route"));
    }

    @Singleton
    static class MyObserver {

        public void registerManagementRoutes(@Observes ManagementInterface mi) {
            mi.router().get("/management/my-route").handler(rc -> rc.response().end("test route"));
        }

    }
}
