package io.quarkus.it.temporal;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class HibernateOrmTemporalTest {

    @Test
    public void testTemporalCurrentState() {
        RestAssured.when().get("/temporal/current/1").then()
                .statusCode(200)
                .body(is("Quarkus Premium T-Shirt"));
    }

    @Test
    public void testTemporalHistoricalState() {
        RestAssured.when().get("/temporal/history/1").then()
                .statusCode(200)
                .body(is("Quarkus T-Shirt"));
    }
}
