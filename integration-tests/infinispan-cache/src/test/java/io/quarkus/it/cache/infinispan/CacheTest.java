package io.quarkus.it.cache.infinispan;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class CacheTest {

    @Test
    public void testCache() {
        when().get("/expensive-resource/invocations").then().statusCode(200).body(is("0"));
        runGetExpensiveRequest();
        runGetExpensiveRequest();
        runGetExpensiveRequest();
        when().get("/expensive-resource/invocations").then().statusCode(200).body(is("1"));

        runDeleteExpensiveRequest();

        runGetExpensiveRequestAsync();
        runGetExpensiveRequestAsync();
        runGetExpensiveRequestAsync();

        when().get("/expensive-resource/invocations").then().statusCode(200).body(is("2"));

        when()
                .delete("/expensive-resource")
                .then()
                .statusCode(204);
    }

    private void runGetExpensiveRequest() {
        when()
                .get("/expensive-resource/I/love/Quarkus?foo=bar")
                .then()
                .statusCode(200)
                .body("result", is("I love Quarkus too!"));
    }

    private void runGetExpensiveRequestAsync() {
        when()
                .get("/expensive-resource/async/I/love/Quarkus?foo=bar")
                .then()
                .statusCode(200)
                .body("result", is("I love Quarkus async too!"));
    }

    private void runDeleteExpensiveRequest() {
        when()
                .delete("/expensive-resource/I/love/Quarkus?foo=bar")
                .then()
                .statusCode(200);
    }
}
