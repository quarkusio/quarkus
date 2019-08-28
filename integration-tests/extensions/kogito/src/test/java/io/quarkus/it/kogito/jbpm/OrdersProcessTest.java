package io.quarkus.it.kogito.jbpm;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class OrdersProcessTest {

    @Test
    public void testRuleEvaluation() {
        RestAssured.when().get("/runProcess").then()
                .body(containsString("OK"));
    }
}
