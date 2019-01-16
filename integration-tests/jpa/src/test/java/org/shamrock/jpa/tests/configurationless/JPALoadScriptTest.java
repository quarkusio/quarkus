package org.shamrock.jpa.tests.configurationless;

import io.restassured.RestAssured;
import org.jboss.shamrock.test.ShamrockTest;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.core.StringContains.containsString;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@RunWith(ShamrockTest.class)
public class JPALoadScriptTest {

    @Test
    public void testImportExecuted() {
        RestAssured.when().get("/jpa-test/import").then()
                .body(containsString("jpa=OK"));
    }
}
