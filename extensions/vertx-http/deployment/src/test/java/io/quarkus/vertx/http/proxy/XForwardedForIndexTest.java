package io.quarkus.vertx.http.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.ForwardedHandlerInitializer;
import io.quarkus.vertx.http.ManagementInterface;
import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;

class XForwardedForIndexTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(ForwardedHandlerInitializer.class, ManagementRoutes.class))
            .withConfiguration("""
                    quarkus.http.proxy.proxy-address-forwarding=true
                    quarkus.http.proxy.allow-x-forwarded=true
                    quarkus.http.proxy.trusted-proxies=127.0.0.1
                    quarkus.http.proxy.x-forwarded-for-index=1
                    quarkus.management.enabled=true
                    quarkus.management.root-path=/management
                    quarkus.management.proxy.proxy-address-forwarding=true
                    quarkus.management.proxy.allow-x-forwarded=true
                    quarkus.management.proxy.trusted-proxies=127.0.0.1
                    quarkus.management.proxy.x-forwarded-for-index=3
                    """);

    @TestHTTPResource
    URL url;

    @TestHTTPResource(value = "/my-route", management = true)
    URL managementRoute;

    @Inject
    Vertx vertx;

    @Test
    void selectsRightmostValueWithoutPort() {
        RestAssured.given().header("X-Forwarded-For", "7.7.7.7, 192.168.42.123").get("/forward").then()
                .statusCode(200)
                .body(Matchers.containsString("192.168.42.123"))
                .body(Matchers.not(Matchers.containsString("7.7.7.7")));
    }

    @Test
    void preservesRightmostIpv4WithPort() {
        RestAssured.given().header("X-Forwarded-For", "7.7.7.7, 192.168.42.123:4444").get("/forward").then()
                .statusCode(200)
                .body(Matchers.containsString("192.168.42.123:4444"));
    }

    @Test
    void preservesRightmostIpv6() {
        RestAssured.given().header("X-Forwarded-For", "1.1.1.1, 2001:db8:85a3::12").get("/forward").then()
                .statusCode(200)
                .body(Matchers.containsString("2001:db8:85a3::12"));
    }

    @Test
    void preservesRightmostIpv6WithPort() {
        RestAssured.given().header("X-Forwarded-For", "1.1.1.1, [2001:db8:85a3:8d3::]:101").get("/forward").then()
                .statusCode(200)
                .body(Matchers.containsString("[2001:db8:85a3:8d3::]:101"));
    }

    @Test
    void trimsWhitespaceAndSkipsEmptyValuesSelectingRightmost() {
        RestAssured.given().header("X-Forwarded-For", " 7.7.7.7 , , 192.168.42.123 ").get("/forward").then()
                .statusCode(200)
                .body(Matchers.containsString("192.168.42.123"))
                .body(Matchers.not(Matchers.containsString("7.7.7.7")));
    }

    @Test
    void skipsLeadingAndTrailingEmptyValuesSelectingRightmost() {
        RestAssured.given().header("X-Forwarded-For", " , , 7.7.7.7, 192.168.42.123, , ").get("/forward").then()
                .statusCode(200)
                .body(Matchers.containsString("192.168.42.123"))
                .body(Matchers.not(Matchers.containsString("7.7.7.7")));
    }

    @Test
    void absentHeaderUsesConnectionPeer() {
        RestAssured.given().get("/forward").then()
                .statusCode(200)
                .body(Matchers.containsString("127.0.0.1"));
    }

    @Test
    void mergesMultipleHeaderLinesSelectingRightmost() {
        var httpClient = vertx.createHttpClient();
        try {
            String body = httpClient
                    .request(HttpMethod.GET, url.getPort(), url.getHost(), "/forward")
                    .compose(request -> {
                        request.putHeader("X-Forwarded-For", "7.7.7.7, 8.8.8.8");
                        request.headers().add("X-Forwarded-For", "9.9.9.9");
                        return request.send();
                    })
                    .compose(HttpClientResponse::body)
                    .await()
                    .toString();
            assertThat(body).contains("9.9.9.9").doesNotContain("8.8.8.8").doesNotContain("7.7.7.7");
        } finally {
            httpClient.close().await();
        }
    }

    @Test
    void selectsThirdValueFromRightOnManagementInterface() {
        RestAssured.given().header("X-Forwarded-For", "0.0.0.0, 192.168.42.123, 2.2.2.2, 3.3.3.3").get(managementRoute)
                .then()
                .statusCode(200)
                .body(Matchers.containsString("192.168.42.123"))
                .body(Matchers.not(Matchers.containsString("0.0.0.0")))
                .body(Matchers.not(Matchers.containsString("3.3.3.3")));
    }

    @Test
    void skipsEmptyValuesSelectingThirdFromRightOnManagementInterface() {
        RestAssured.given().header("X-Forwarded-For", "1.1.1.1, , 192.168.42.123, , 2.2.2.2, 3.3.3.3").get(managementRoute)
                .then()
                .statusCode(200)
                .body(Matchers.containsString("192.168.42.123"))
                .body(Matchers.not(Matchers.containsString("1.1.1.1")))
                .body(Matchers.not(Matchers.containsString("2.2.2.2")))
                .body(Matchers.not(Matchers.containsString("3.3.3.3")));
    }

    @Test
    void rejectsWhenFewerValuesThanIndexOnManagementInterface() {
        RestAssured.given().header("X-Forwarded-For", "1.1.1.1, 2.2.2.2").get(managementRoute).then()
                .statusCode(400);
    }

    @Singleton
    static class ManagementRoutes {
        public void register(@Observes ManagementInterface mi) {
            mi.router().get("/management/my-route")
                    .handler(rc -> rc.response().end(rc.request().remoteAddress().toString()));
        }
    }
}
