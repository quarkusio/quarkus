package io.quarkus.it.cache;

import static io.quarkus.it.cache.ExpensiveResource.EXPENSIVE_RESOURCE_CACHE_NAME;
import static io.quarkus.it.cache.GetIfPresentResource.GET_IF_PRESENT_CACHE_NAME;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@DisplayName("Tests the cache extension")
public class CacheTestCase {

    @Test
    void testCache() {
        assertMetrics(EXPENSIVE_RESOURCE_CACHE_NAME, 0, 0, 0);

        runExpensiveRequest();
        assertMetrics(EXPENSIVE_RESOURCE_CACHE_NAME, 1, 1, 0);

        runExpensiveRequest();
        assertMetrics(EXPENSIVE_RESOURCE_CACHE_NAME, 1, 1, 1);

        runExpensiveRequest();
        assertMetrics(EXPENSIVE_RESOURCE_CACHE_NAME, 1, 1, 2);

        when().get("/expensive-resource/invocations").then().statusCode(200).body(is("1"));
    }

    private void runExpensiveRequest() {
        when().get("/expensive-resource/I/love/Quarkus?foo=bar").then().statusCode(200).body("result",
                is("I love Quarkus too!"));
    }

    @Test
    void testGetIfPresentMetrics() {
        assertMetrics(GET_IF_PRESENT_CACHE_NAME, 0, 0, 0);

        String cacheKey = "foo";
        String cacheValue = "bar";

        given().pathParam("key", cacheKey)
                .when().get("/get-if-present/{key}")
                .then().statusCode(204);
        assertMetrics(GET_IF_PRESENT_CACHE_NAME, 0, 1, 0);

        given().pathParam("key", cacheKey)
                .when().get("/get-if-present/{key}")
                .then().statusCode(204);
        assertMetrics(GET_IF_PRESENT_CACHE_NAME, 0, 2, 0);

        given().pathParam("key", cacheKey).body(cacheValue)
                .when().put("/get-if-present/{key}")
                .then().statusCode(204);
        assertMetrics(GET_IF_PRESENT_CACHE_NAME, 1, 2, 0);

        given().pathParam("key", cacheKey)
                .when().get("/get-if-present/{key}")
                .then().statusCode(200).body(is(cacheValue));
        assertMetrics(GET_IF_PRESENT_CACHE_NAME, 1, 2, 1);

        given().pathParam("key", cacheKey)
                .when().get("/get-if-present/{key}")
                .then().statusCode(200).body(is(cacheValue));
        assertMetrics(GET_IF_PRESENT_CACHE_NAME, 1, 2, 2);
    }

    private void assertMetrics(String cacheName, double expectedPuts, double expectedMisses, double expectedHits) {
        String metricsResponse = when().get("/q/metrics").then().extract().asString();
        assertTrue(metricsResponse.contains(String.format("cache_puts_total{cache=\"%s\"} %.1f", cacheName, expectedPuts)));
        assertTrue(metricsResponse
                .contains(String.format("cache_gets_total{cache=\"%s\",result=\"miss\"} %.1f", cacheName, expectedMisses)));
        assertTrue(metricsResponse
                .contains(String.format("cache_gets_total{cache=\"%s\",result=\"hit\"} %.1f", cacheName, expectedHits)));
    }
}
