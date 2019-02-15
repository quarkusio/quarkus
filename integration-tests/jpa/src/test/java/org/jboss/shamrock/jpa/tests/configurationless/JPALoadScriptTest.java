package org.jboss.shamrock.jpa.tests.configurationless;

import static org.hamcrest.core.StringContains.containsString;

import org.jboss.shamrock.test.junit.ShamrockTest;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@ShamrockTest
public class JPALoadScriptTest {

    @Test
    public void testImportExecuted() {
        RestAssured.when().get("/jpa-test/import").then()
                .body(containsString("jpa=OK"));
    }
}
