package io.quarkus.it.optaplanner;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test various OptaPlanner operations running in Quarkus
 */

@QuarkusTest
public class OptaPlannerTestResourceTest {

    @Test
    @Timeout(600)
    public void solveWithSolverFactory() throws Exception {
        RestAssured.given()
                .header("Content-Type", "application/json")
                .when()
                .body("{\"valueList\":[\"v1\",\"v2\"],\"entityList\":[{},{}]}")
                .post("/optaplanner/test/solver-factory")
                .then()
                .body(is(
                        "{\"valueList\":[\"v1\",\"v2\"],\"entityList\":[{\"value\":\"v1\"},{\"value\":\"v2\"}],\"score\":\"0\"}"));
    }

}
