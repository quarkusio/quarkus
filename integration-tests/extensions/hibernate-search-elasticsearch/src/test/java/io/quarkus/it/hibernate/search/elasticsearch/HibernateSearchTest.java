package io.quarkus.it.hibernate.search.elasticsearch;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class HibernateSearchTest {

    @Test
    public void testSearch() throws Exception {
        RestAssured.when().put("/test/hibernate-search/init-data").then()
                .statusCode(204);

        RestAssured.when().get("/test/hibernate-search/search").then()
                .statusCode(200)
                .body(is("OK"));

        RestAssured.when().put("/test/hibernate-search/mass-indexer").then()
                .statusCode(200)
                .body(is("OK"));
    }
}
