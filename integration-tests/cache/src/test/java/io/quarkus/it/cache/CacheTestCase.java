package io.quarkus.it.cache;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@DisplayName("Tests the cache extension")
public class CacheTestCase {

    @Test
    public void testCache() {
        runExpensiveRequest();
        runExpensiveRequest();
        RestAssured.given().when().get("/expensive-resource/invocations").then().statusCode(200).body(is("1"));
    }

    private void runExpensiveRequest() {
        RestAssured.given().when().get("/expensive-resource/I/love/Quarkus?foo=bar").then().statusCode(200).body("result",
                is("I love Quarkus too!"));
    }
}
