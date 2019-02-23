package io.quarkus.jpa.tests.configurationless;

import static org.hamcrest.core.StringContains.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@QuarkusTest
public class JPAOnStartupTest {

    @Test
    public void testStartupJpa() {
        RestAssured.when().get("/jpa-test/cake").then()
                .body(containsString("Chocolate"));
    }
}
