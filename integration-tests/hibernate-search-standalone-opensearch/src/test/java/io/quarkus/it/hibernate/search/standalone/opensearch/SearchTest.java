package io.quarkus.it.hibernate.search.standalone.opensearch;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class SearchTest {

    @Test
    public void testSearch() {
        RestAssured.when().put("/test/search/init-data").then()
                .statusCode(204);

        RestAssured.when().get("/test/search/search").then()
                .statusCode(200)
                .body(is("OK"));

        RestAssured.when().get("/test/search/search-projection").then()
                .statusCode(200)
                .body(is("OK"));
    }
}
