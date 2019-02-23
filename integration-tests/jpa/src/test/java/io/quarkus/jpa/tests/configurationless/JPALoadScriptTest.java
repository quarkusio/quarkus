package io.quarkus.jpa.tests.configurationless;

import static org.hamcrest.core.StringContains.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@QuarkusTest
public class JPALoadScriptTest {

    @Test
    public void testImportExecuted() {
        RestAssured.when().get("/jpa-test/import").then()
                .body(containsString("jpa=OK"));
    }
}
