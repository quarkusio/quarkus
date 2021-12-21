package io.quarkus.it.cache;

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
    public void testCache() {
        runExpensiveRequest();
        runExpensiveRequest();
        runExpensiveRequest();
        when().get("/expensive-resource/invocations").then().statusCode(200).body(is("1"));

        String metricsResponse = when().get("/q/metrics").then().extract().asString();
        assertTrue(metricsResponse.contains("cache_puts_total{cache=\"expensiveResourceCache\",} 1.0"));
        assertTrue(metricsResponse.contains("cache_gets_total{cache=\"expensiveResourceCache\",result=\"miss\",} 1.0"));
        assertTrue(metricsResponse.contains("cache_gets_total{cache=\"expensiveResourceCache\",result=\"hit\",} 2.0"));
    }

    private void runExpensiveRequest() {
        when().get("/expensive-resource/I/love/Quarkus?foo=bar").then().statusCode(200).body("result",
                is("I love Quarkus too!"));
    }
}
