package io.quarkus.hibernate.orm.quoting_strategies;

import static org.hamcrest.core.StringContains.containsString;

import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;

public abstract class AbstractJPAQuotedTest {

    @Test
    public void testQuotedIdentifiers() {
        RestAssured.when().post("/jpa-test-quoted").then().body(containsString("ok"));

        RestAssured.when().get("/jpa-test-quoted").then().body(containsString("group_name"),
                containsString("group_value"));
    }

}
