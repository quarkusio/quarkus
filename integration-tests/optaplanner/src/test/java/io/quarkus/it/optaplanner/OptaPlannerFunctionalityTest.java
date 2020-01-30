package io.quarkus.it.optaplanner;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test various OptaPlanner operations running in Quarkus
 */

@QuarkusTest
public class OptaPlannerFunctionalityTest {

    @Test
    public void solveWithSolverFactory() throws Exception {
        RestAssured.given()
                .header("Content-Type", "application/json")
                .when()
                .body("{\"valueList\":[\"v1\",\"v2\"],\"entityList\":[{},{}]}")
                .post("/optaplanner/test/solver-factory");
        //                .then()
        //                .body(is("{\"entityList\":[{\"value\":\"v1\"},{\"value\":\"v2\"}],\"score\":0,\"valueList\":[\"v1\",\"v2\"]}"));
    }

}
