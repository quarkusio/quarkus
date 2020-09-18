package io.quarkus.it.micrometer.jmx;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Test functioning prometheus endpoint.
 * Use test execution order to ensure one http server request measurement
 * is present when the endpoint is scraped.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JmxMetricsRegistryTest {

    @Test
    @Order(1)
    void testRegistryInjection() {
        given()
                .when().get("/message/ping")
                .then()
                .statusCode(200)
                .body(containsString("io.micrometer.jmx.JmxMeterRegistry"));
    }

    @Test
    @Order(2)
    void testUnknownUrl() {
        given()
                .when().get("/messsage/notfound")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(3)
    void testServerError() {
        given()
                .when().get("/message/fail")
                .then()
                .statusCode(500);
    }

    @Test
    @Order(4)
    void testPathParameter() {
        given()
                .when().get("/message/item/123")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(10)
    void testJmxReporter() {

        given()
                .when().get("/message/mbeans")
                .then()
                .log().all()
                .statusCode(200)

                // JMX endpoint is returning a subset of mbean objects to inspect
                // hierarchical naming means all tags are present: registry=jmx, and env=test

                // Generic connection statistic
                .body(containsString("metrics:name=httpServerConnections.env.test.registry.jmx.statistic"))

                .body(containsString(
                        "metrics:name=httpServerRequests.env.test.method.GET.outcome.CLIENT_ERROR.registry.jmx.status.404.uri.NOT_FOUND"))
                .body(containsString(
                        "metrics:name=httpServerRequests.env.test.method.GET.outcome.SERVER_ERROR.registry.jmx.status.500.uri./message/fail"))

                .body(containsString(
                        "metrics:name=httpServerRequests.env.test.method.GET.outcome.SUCCESS.registry.jmx.status.200.uri./message/ping"))
                .body(containsString(
                        "metrics:name=httpServerRequests.env.test.method.GET.outcome.SUCCESS.registry.jmx.status.200.uri./message/item/{id}"))

                // this was defined by a tag to a non-matching registry, and should not be found
                .body(not(containsString("class-should-not-match")));
    }
}
