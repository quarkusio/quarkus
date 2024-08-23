package io.quarkus.it.cache.infinispan;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class CacheTest {

    @Test
    public void testCache() {
        runExpensiveRequest();
        runExpensiveRequest();
        runExpensiveRequest();
        when().get("/expensive-resource/invocations").then().statusCode(200).body(is("1"));

        when()
                .post("/expensive-resource")
                .then()
                .statusCode(204);
    }

    private void runExpensiveRequest() {
        when()
                .get("/expensive-resource/I/love/Quarkus?foo=bar")
                .then()
                .statusCode(200)
                .body("result", is("I love Quarkus too!"));
    }
}
