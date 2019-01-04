package org.shamrock.jpa.tests.configurationless;

import static org.hamcrest.core.StringContains.containsString;

import org.jboss.shamrock.test.ShamrockTest;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.RestAssured;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@RunWith(ShamrockTest.class)
public class JPAConfigurationlessTest {

    @Test
    public void testInjection() {
        RestAssured.when().get("/jpa-test").then()
                .body(containsString("jpa=OK"));

        RestAssured.when().get("/jpa-test/user-tx").then()
                .body(containsString("jpa=OK"));
    }
}
