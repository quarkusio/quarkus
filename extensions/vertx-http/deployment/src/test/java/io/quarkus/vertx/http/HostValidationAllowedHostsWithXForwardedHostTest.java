package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;

import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class HostValidationAllowedHostsWithXForwardedHostTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(LocalBeanRegisteringRoute.class)
                    .addAsResource(new StringAsset("""
                            quarkus.http.proxy.proxy-address-forwarding=true\n
                            quarkus.http.proxy.allow-x-forwarded=true\n
                            quarkus.http.proxy.enable-forwarded-host=true\n
                            quarkus.http.host-validation.allowed-hosts=public-api.com
                            """), "application.properties"));

    @Test
    void somePublicApiHostRequest() {
        given().header("Host", "some-public-api")
                .get("/test").then()
                .statusCode(403)
                .body(emptyString());
    }

    @Test
    void somePublicApiHostWithXForwardedHostRequest() {
        given().header("Host", "some-public-api")
                .header("X-Forwarded-Host", "public-api.com")
                .get("/test").then()
                .statusCode(200)
                .body(equalTo("test route"));
    }

    static class LocalBeanRegisteringRoute {

        void init(@Observes Router router) {
            Handler<RoutingContext> handler = rc -> rc.response().end("test route");

            router.get("/test").handler(handler);
        }
    }
}
