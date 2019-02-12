package org.shamrock.jpa.tests.configurationless;

import static org.hamcrest.core.StringContains.containsString;

import org.jboss.shamrock.test.junit.ShamrockTest;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@ShamrockTest
public class JPAOnStartupTest {

    @Test
    public void testStartupJpa() {
        RestAssured.when().get("/jpa-test/cake").then()
                .body(containsString("Chocolate"));
    }
}
