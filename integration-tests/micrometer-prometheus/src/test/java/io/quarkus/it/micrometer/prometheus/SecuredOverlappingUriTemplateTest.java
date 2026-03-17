package io.quarkus.it.micrometer.prometheus;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Reproducer for https://github.com/quarkusio/quarkus/issues/53030
 */
@QuarkusTest
class SecuredOverlappingUriTemplateTest {

    @Test
    void unauthorizedRequestWithOverlappingControllers_metricsAreTemplated() {
        when().get("/secured/test123/details").then().statusCode(401);

        given().header("Accept", "text/plain")
                .when().get("/q/metrics")
                .then().statusCode(200)
                .body(
                        containsString("uri=\"/secured/{id}/details\""),
                        not(containsString("uri=\"/secured/test123/details\"")));
    }

    @Test
    void unauthorizedRequestWithDeeplyOverlappingControllers_metricsAreTemplated() {
        when().get("/secured/test456/info/summary").then().statusCode(401);

        given().header("Accept", "text/plain")
                .when().get("/q/metrics")
                .then().statusCode(200)
                .body(
                        containsString("uri=\"/secured/{id}/info/{section}\""),
                        not(containsString("uri=\"/secured/test456/info/summary\"")));
    }
}
