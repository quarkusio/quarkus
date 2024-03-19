package io.quarkus.it.hibernate.search.standalone.elasticsearch;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AnalysisTest {

    @Test
    public void testSearch() {
        RestAssured.when().put("/test/analysis/init-data").then()
                .statusCode(204);

        RestAssured.when().get("/test/analysis/analysis-configured").then()
                .statusCode(200)
                .body(is("OK"));
    }
}
