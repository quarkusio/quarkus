package io.quarkus.it.spring.data.jpa;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class CatalogValueResourceTest {

    @Test
    void findByKey() {
        when().get("/catalog-value/super/DE-SN").then()
                .statusCode(200)
                .body(containsString("Saxony"))
                .body(containsString("DE-SN"));
    }

    @Test
    void findFederalStateByKey() {
        when().get("/catalog-value/federal-state/DE-SN").then()
                .statusCode(200)
                .body(containsString("Saxony"))
                .body(containsString("DE-SN"));
    }
}
