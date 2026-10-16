package io.quarkus.vertx.http.management;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.net.URL;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.ManagementInterface;

public class ManagementHostValidationWithXForwardedHostTest {

    private static final String configuration = "quarkus.management.enabled=true\n"
            + "quarkus.management.root-path=/management\n"
            + "quarkus.management.proxy.proxy-address-forwarding=true\n"
            + "quarkus.management.proxy.allow-x-forwarded=true\n"
            + "quarkus.management.proxy.enable-forwarded-host=true\n"
            + "quarkus.management.host-validation.allowed-hosts=public-api.com\n";

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(configuration), "application.properties")
                    .addClasses(MyObserver.class));

    @TestHTTPResource(value = "/my-route", management = true)
    URL myRoute;

    @TestHTTPResource(value = "/remote-addr", management = true)
    URL remoteAddrRoute;

    @Test
    void somePublicApiHostRequest() {
        given().header("Host", "some-public-api")
                .get(myRoute).then()
                .statusCode(400)
                .body(emptyString());
    }

    @Test
    void somePublicApiHostWithXForwardedHostRequest() {
        given().header("Host", "some-public-api")
                .header("X-Forwarded-Host", "public-api.com")
                .get(myRoute).then()
                .statusCode(200)
                .body(equalTo("test route"));
    }

    @Test
    void xForwardedForSetsRemoteAddressOnManagement() {
        given().header("X-Forwarded-Host", "public-api.com")
                .header("X-Forwarded-For", "192.168.42.123")
                .get(remoteAddrRoute).then()
                .statusCode(200)
                .body(containsString("192.168.42.123"));
    }

    @Test
    void xForwardedForUsesLeftmostValueByDefaultOnManagement() {
        given().header("X-Forwarded-Host", "public-api.com")
                .header("X-Forwarded-For", "1.1.1.1, 2.2.2.2")
                .get(remoteAddrRoute).then()
                .statusCode(200)
                .body(containsString("1.1.1.1"))
                .body(not(containsString("2.2.2.2")));
    }

    @Singleton
    static class MyObserver {

        public void registerManagementRoutes(@Observes ManagementInterface mi) {
            mi.router().get("/management/my-route").handler(rc -> rc.response().end("test route"));
            mi.router().get("/management/remote-addr")
                    .handler(rc -> rc.response().end(rc.request().remoteAddress().toString()));
        }
    }
}
