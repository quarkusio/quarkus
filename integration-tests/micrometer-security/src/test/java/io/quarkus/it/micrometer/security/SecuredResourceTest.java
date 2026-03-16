package io.quarkus.it.micrometer.security;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class SecuredResourceTest {

    @Test
    void testMetricsForUnauthorizedRequest() {
        when().get("/secured/foo")
                .then()
                .statusCode(403);

        when().get("/q/metrics")
                .then()
                .statusCode(200)
                .body(
                        allOf(
                                not(containsString("/secured/foo")),
                                containsString("/secured/{message}"))

                );
    }

    @Test
    void testMetricsForUnauthorizedRequestWithOverlappingPaths() {
        // When two controllers have overlapping paths (e.g. /secured/{message} and /secured/{message}/details),
        // the URI template should still be resolved correctly for unauthorized requests.
        // See https://github.com/quarkusio/quarkus/issues/53030
        when().get("/secured/foo")
                .then()
                .statusCode(403);

        when().get("/secured/foo/details")
                .then()
                .statusCode(403);

        when().get("/q/metrics")
                .then()
                .statusCode(200)
                .body(
                        allOf(
                                not(containsString("/secured/foo")),
                                containsString("/secured/{message}"),
                                containsString("/secured/{message}/details")));
    }

}
