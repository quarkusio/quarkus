package io.quarkus.jpa.tests.configurationless;

import static org.hamcrest.core.StringContains.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@QuarkusTest
public class JPAConfigurationlessTest {

    @Test
    public void testInjection() {
        RestAssured.when().get("/jpa-test").then()
                .body(containsString("jpa=OK"));

        RestAssured.when().get("/jpa-test/user-tx").then()
                .body(containsString("jpa=OK"));
    }
}
