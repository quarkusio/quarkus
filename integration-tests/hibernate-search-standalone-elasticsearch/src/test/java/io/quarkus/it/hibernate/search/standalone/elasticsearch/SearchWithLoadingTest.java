package io.quarkus.it.hibernate.search.standalone.elasticsearch;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class SearchWithLoadingTest {

    @Test
    public void testSearch() {
        RestAssured.when().put("/test/search-with-loading/init-data").then()
                .statusCode(204);

        RestAssured.when().get("/test/search-with-loading/search").then()
                .statusCode(200)
                .body(is("OK"));

        RestAssured.when().put("/test/search-with-loading/purge").then()
                .statusCode(200)
                .body(is("OK"));

        RestAssured.when().put("/test/search-with-loading/refresh").then()
                .statusCode(200)
                .body(is("OK"));

        RestAssured.when().get("/test/search-with-loading/search-empty").then()
                .statusCode(200);

        RestAssured.when().put("/test/search-with-loading/mass-indexer").then()
                .statusCode(200)
                .body(is("OK"));

        RestAssured.when().get("/test/search-with-loading/search").then()
                .statusCode(200)
                .body(is("OK"));
    }
}
