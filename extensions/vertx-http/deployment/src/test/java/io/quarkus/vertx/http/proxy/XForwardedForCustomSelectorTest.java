package io.quarkus.vertx.http.proxy;

import java.net.URL;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.ForwardedHandlerInitializer;
import io.quarkus.vertx.http.ManagementInterface;
import io.restassured.RestAssured;

class XForwardedForCustomSelectorTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(ForwardedHandlerInitializer.class, CustomXForwardedForSelector.class,
                    ManagementRoutes.class))
            .withConfiguration("""
                    quarkus.http.proxy.proxy-address-forwarding=true
                    quarkus.http.proxy.allow-x-forwarded=true
                    quarkus.http.proxy.trusted-proxies=127.0.0.1
                    quarkus.management.enabled=true
                    quarkus.management.root-path=/management
                    quarkus.management.proxy.proxy-address-forwarding=true
                    quarkus.management.proxy.allow-x-forwarded=true
                    quarkus.management.proxy.trusted-proxies=127.0.0.1
                    """);

    @TestHTTPResource(value = "/my-route", management = true)
    URL managementRoute;

    @Test
    void beanSelectsValue() {
        RestAssured.given().header("X-Forwarded-For", "7.7.7.7, 1.2.3.4").get("/forward").then()
                .statusCode(200)
                .body(Matchers.containsString("1.2.3.4"))
                .body(Matchers.not(Matchers.containsString("7.7.7.7")));
    }

    @Test
    void beanCanReturnConnectionPeer() {
        RestAssured.given().header("X-Forwarded-For", "7.7.7.7, 1.2.3.4").header("X-Test-Peer", "true").get("/forward")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("127.0.0.1"));
    }

    @Test
    void beanCanRejectWithBadRequest() {
        RestAssured.given().header("X-Forwarded-For", "7.7.7.7, 1.2.3.4").header("X-Test-Reject", "true").get("/forward")
                .then()
                .statusCode(400);
    }

    @Test
    void beanCanReadOtherHeaders() {
        RestAssured.given().header("X-Forwarded-For", "7.7.7.7, 1.2.3.4").header("X-Test-Header", "true")
                .header("CF-Connecting-IP", "9.9.9.9").get("/forward").then()
                .statusCode(200)
                .body(Matchers.containsString("9.9.9.9"));
    }

    @Test
    void beanFailsClosedWhenItThrows() {
        RestAssured.given().header("X-Forwarded-For", "7.7.7.7, 1.2.3.4").header("X-Test-Throw", "true").get("/forward")
                .then()
                .statusCode(400);
    }

    @Test
    void beanRejectsEmptyReturnValueWithBadRequest() {
        RestAssured.given().header("X-Forwarded-For", "7.7.7.7, 1.2.3.4").header("X-Test-Empty", "true").get("/forward")
                .then()
                .statusCode(400);
    }

    @Test
    void beanRejectsBlankReturnValueWithBadRequest() {
        RestAssured.given().header("X-Forwarded-For", "7.7.7.7, 1.2.3.4").header("X-Test-Blank", "true").get("/forward")
                .then()
                .statusCode(400);
    }

    @Test
    void beanReturnValueIsTrimmedBeforeUse() {
        RestAssured.given().header("X-Forwarded-For", "7.7.7.7, 1.2.3.4").header("X-Test-Padded", "true").get("/forward")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("|1.2.3.4:"))
                .body(Matchers.not(Matchers.containsString("  1.2.3.4  ")));
    }

    @Test
    void beanNotInvokedWhenHeaderAbsent() {
        RestAssured.given().header("X-Test-Reject", "true").get("/forward").then()
                .statusCode(200)
                .body(Matchers.containsString("127.0.0.1"));
    }

    @Test
    void beanSelectsValueOnManagementInterface() {
        RestAssured.given().header("X-Forwarded-For", "7.7.7.7, 1.2.3.4").get(managementRoute).then()
                .statusCode(200)
                .body(Matchers.containsString("1.2.3.4"))
                .body(Matchers.not(Matchers.containsString("7.7.7.7")));
    }

    @Singleton
    static class ManagementRoutes {
        public void register(@Observes ManagementInterface mi) {
            mi.router().get("/management/my-route")
                    .handler(rc -> rc.response().end(rc.request().remoteAddress().toString()));
        }
    }
}
