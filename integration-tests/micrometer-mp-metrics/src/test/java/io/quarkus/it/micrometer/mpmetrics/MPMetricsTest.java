package io.quarkus.it.micrometer.mpmetrics;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MPMetricsTest {

    @Test
    @Order(1)
    void callPrimeGen_1() {
        given()
                .when().get("/prime/31")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(2)
    void callPrimeGen_2() {
        given()
                .when().get("/prime/33")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(3)
    void callPrimeGen_3() {
        given()
                .when().get("/prime/887")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(4)
    void validateMetricsOutput_1() {
        given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200)

                // Prometheus body has ALL THE THINGS in no particular order

                // PrimeResource
                .body(containsString(
                        "io_quarkus_it_micrometer_mpmetrics_PrimeResource_PrimeResource_total{scope=\"application\",} 1.0"))
                .body(containsString(
                        "io_quarkus_it_micrometer_mpmetrics_CountedInstance_CountedInstance_total{scope=\"application\",} 2.0"))

                // number of concurrent requests at time of sample
                .body(containsString(
                        "io_quarkus_it_micrometer_mpmetrics_PrimeResource_checkIfPrime{scope=\"application\",} 0.0"))
                .body(containsString(
                        "io_quarkus_it_micrometer_mpmetrics_CountedInstance_countPrimes_total{scope=\"application\",} 2.0"))
                .body(containsString(
                        "highestPrimeNumberSoFar 887.0"))
                .body(containsString(
                        "io_quarkus_it_micrometer_mpmetrics_PrimeResource_highestPrimeNumberSoFar2{scope=\"application\",} 887.0"))

                // the counter associated with a timed method should have been removed
                .body(not(containsString("io_quarkus_it_micrometer_mpmetrics_PrimeResource_checkPrime")));
    }

    @Test
    @Order(5)
    void callPrimeGen_4() {
        given()
                .when().get("/prime/900")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(8)
    void callMessage() {
        given()
                .when().get("/message")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(9)
    void validateMetricsOutput_2() {
        given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200)

                // Prometheus body has ALL THE THINGS in no particular order
                .body(containsString(
                        "io_quarkus_it_micrometer_mpmetrics_CountedInstance_countPrimes_total{scope=\"application\",} 2.0"))
                .body(containsString(
                        "highestPrimeNumberSoFar 887.0"))
                .body(containsString(
                        "io_quarkus_it_micrometer_mpmetrics_InjectedInstance_notPrime_total{scope=\"application\",}"))
                .body(not(containsString("/message")));
    }

    @Test
    @Order(10)
    void validateJsonOutput() {
        given()
                .header("Accept", "application/json")
                .when().get("/q/metrics")
                .then()
                .statusCode(200)
                .body("'io.quarkus.it.micrometer.mpmetrics.CountedInstance.countPrimes;scope=application'",
                        Matchers.equalTo(2.0f))
                .body("'highestPrimeNumberSoFar'",
                        Matchers.equalTo(887.0f));
    }

}
