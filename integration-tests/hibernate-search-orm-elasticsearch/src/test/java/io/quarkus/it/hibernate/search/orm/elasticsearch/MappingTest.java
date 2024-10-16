package io.quarkus.it.hibernate.search.orm.elasticsearch;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class MappingTest {

    @Test
    public void testMapping() {
        RestAssured.when().put("/test/mapping/init-data").then()
                .statusCode(204);

        RestAssured.when().get("/test/mapping/mapping-property").then()
                .statusCode(200)
                .body(is("OK"));
    }
}
