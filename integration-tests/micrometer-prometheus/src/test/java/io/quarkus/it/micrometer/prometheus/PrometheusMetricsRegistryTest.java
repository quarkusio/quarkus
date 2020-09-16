package io.quarkus.it.micrometer.prometheus;

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
class PrometheusMetricsRegistryTest {

    @Test
    @Order(1)
    void testRegistryInjection() {
        given()
                .when().get("/message")
                .then()
                .statusCode(200)
                .body(containsString("io.micrometer.core.instrument.composite.CompositeMeterRegistry"));
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
    @Order(3)
    void testPathParameter() {
        given()
                .when().get("/message/item/123")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(4)
    void testPanacheCalls() {
        given()
                .when().get("/fruit/create")
                .then()
                .statusCode(204);

        given()
                .when().get("/fruit/all")
                .then()
                .statusCode(204);
    }

    @Test
    @Order(5)
    void testPrimeEndpointCalls() {
        given()
                .when().get("/prime/7")
                .then()
                .statusCode(200)
                .body(containsString("is prime"));
    }

    @Test
    @Order(10)
    void testPrometheusScrapeEndpoint() {
        given()
                .when().get("/metrics")
                .then()
                .log().body()
                .statusCode(200)

                // Prometheus body has ALL THE THINGS in no particular order

                .body(containsString("registry=\"prometheus\""))
                .body(containsString("env=\"test\""))
                .body(containsString("http_server_requests"))

                .body(containsString("status=\"404\""))
                .body(containsString("uri=\"NOT_FOUND\""))
                .body(containsString("outcome=\"CLIENT_ERROR\""))

                .body(containsString("status=\"500\""))
                .body(containsString("uri=\"/message/fail\""))
                .body(containsString("outcome=\"SERVER_ERROR\""))

                .body(containsString("status=\"200\""))
                .body(containsString("uri=\"/message\""))
                .body(containsString("uri=\"/message/item/{id}\""))
                .body(containsString("outcome=\"SUCCESS\""))

                // Verify Hibernate Metrics
                .body(containsString(
                        "hibernate_sessions_open_total{entityManagerFactory=\"<default>\",env=\"test\",registry=\"prometheus\",} 2.0"))
                .body(containsString(
                        "hibernate_sessions_closed_total{entityManagerFactory=\"<default>\",env=\"test\",registry=\"prometheus\",} 2.0"))
                .body(containsString(
                        "hibernate_connections_obtained_total{entityManagerFactory=\"<default>\",env=\"test\",registry=\"prometheus\",}"))
                .body(containsString(
                        "hibernate_entities_inserts_total{entityManagerFactory=\"<default>\",env=\"test\",registry=\"prometheus\",} 3.0"))
                .body(containsString(
                        "hibernate_flushes_total{entityManagerFactory=\"<default>\",env=\"test\",registry=\"prometheus\",} 2.0"))

                // this was defined by a tag to a non-matching registry, and should not be found
                .body(not(containsString("class-should-not-match")))

                // should not find this ignored uri
                .body(not(containsString("uri=\"/fruit/create\"")));
    }
}
