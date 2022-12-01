package io.quarkus.it.cache;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@DisplayName("Tests the integration between the cache and the rest-client extensions")
public class RestClientTestCase {

    private static final String CITY = "Toulouse";
    private static final String TODAY = "2020-12-20";

    @Test
    public void test() {
        assertInvocations("0");
        getSunriseTimeInvocations();
        assertInvocations("1");
        getSunriseTimeInvocations();
        assertInvocations("1");
        invalidate();
        getSunriseTimeInvocations();
        assertInvocations("2");
        invalidateAll();
        getSunriseTimeInvocations();
        assertInvocations("3");
    }

    private void assertInvocations(String expectedInvocations) {
        given()
                .when()
                .get("/rest-client/invocations")
                .then()
                .statusCode(200)
                .body(equalTo(expectedInvocations));
    }

    private void getSunriseTimeInvocations() {
        given()
                .queryParam("date", TODAY)
                .when()
                .get("/rest-client/time/{city}", CITY)
                .then()
                .statusCode(200);
    }

    private void invalidate() {
        given()
                .queryParam("date", TODAY)
                .queryParam("notPartOfTheCacheKey", "notPartOfTheCacheKey")
                .when()
                .delete("/rest-client/invalidate/{city}", CITY)
                .then()
                .statusCode(204);
    }

    private void invalidateAll() {
        given()
                .when()
                .delete("/rest-client/invalidate")
                .then()
                .statusCode(204);
    }
}
