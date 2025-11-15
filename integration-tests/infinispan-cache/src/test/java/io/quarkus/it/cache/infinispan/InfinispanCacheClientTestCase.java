package io.quarkus.it.cache.infinispan;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Headers;

@QuarkusTest
@DisplayName("Tests the integration between the infinispan cache and the rest-client extensions")
public class InfinispanCacheClientTestCase {

    private static final String CITY = "Toulouse";
    private static final String TODAY = "2020-12-20";

    @Test
    public void test() {
        assertInvocations("0");
        getSunriseTimeInvocations();
        assertInvocations("1");
        getSunriseTimeInvocations();
        assertInvocations("1");
        getAsyncSunriseTimeInvocations();
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
        doGetSunriseTimeInvocations("/rest-client/time/{city}", true);
    }

    private void getAsyncSunriseTimeInvocations() {
        doGetSunriseTimeInvocations("/rest-client/async/time/{city}", false);
    }

    private void doGetSunriseTimeInvocations(String path, Boolean blockingAllowed) {
        Headers headers = given()
                .queryParam("date", TODAY)
                .when()
                .get(path, CITY)
                .then()
                .statusCode(200)
                .extract().headers();
        assertEquals(headers.get("before").getValue(), headers.get("after").getValue());
        assertEquals(blockingAllowed.toString(), headers.get("blockingAllowed").getValue());
    }

    private void invalidate() {
        Headers headers = given()
                .queryParam("date", TODAY)
                .queryParam("notPartOfTheCacheKey", "notPartOfTheCacheKey")
                .when()
                .delete("/rest-client/invalidate/{city}", CITY)
                .then()
                .statusCode(204)
                .extract().headers();
        assertNotNull(headers.get("incoming").getValue());
        assertEquals("false", headers.get("blockingAllowed").getValue());
    }

    private void invalidateAll() {
        given()
                .when()
                .delete("/rest-client/invalidate")
                .then()
                .statusCode(204);
    }
}
