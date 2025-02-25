package io.quarkus.it.main;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class DynamicTestWithJacksonTestCase {

    @TestFactory
    public DynamicTest test() {
        return dynamicTest("restAssuredExample", () -> given()
                .body(new Data()) // Uses TCCL to load Jackson
        );
    }

    static class Data {
        public String data;
    }
}