package io.quarkus.it.jpa.mssql;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test connecting Hibernate ORM to MS SQL.
 * See the README.md for reminders about how to quickly start a Docker container running MS SQL Server.
 */
@QuarkusTest
public class JPAFunctionalityTest {

    @Test
    public void testJPAFunctionalityFromServlet() throws Exception {
        RestAssured.when().get("/jpa-mssql/testfunctionality").then().body(is("OK"));
    }

}
