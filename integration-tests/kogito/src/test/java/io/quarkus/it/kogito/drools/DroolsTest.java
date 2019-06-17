package io.quarkus.it.kogito.drools;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class DroolsTest {

    @Test
    public void testRuleEvaluation() {
        RestAssured.when().get("/hello").then()
                .body(containsString("Mario is older than Mark"));
    }

    @Test
    public void testTooYoung() {
        RestAssured.when().get("/candrink/Mark/17").then()
                .body(containsString("Mark can NOT drink"));
    }

    @Test
    public void testAdult() {
        RestAssured.when().get("/candrink/Mario/18").then()
                .body(containsString("Mario can drink"));
    }
}
