package io.quarkus.it.audit;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class HibernateOrmAuditTest {

    @Test
    public void testAuditCurrentState() {
        RestAssured.when().get("/audit/current/1").then()
                .statusCode(200)
                .body(is("Updated Order"));
    }

    @Test
    public void testAuditHistoricalState() {
        RestAssured.when().get("/audit/history/1").then()
                .statusCode(200)
                .body(is("Initial Order"));
    }

    @Test
    public void testAuditChangesetCount() {
        RestAssured.when().get("/audit/changesets/1").then()
                .statusCode(200)
                .body(is("2"));
    }
}
