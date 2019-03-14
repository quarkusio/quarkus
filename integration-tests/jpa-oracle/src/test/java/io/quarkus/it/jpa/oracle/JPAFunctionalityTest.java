package io.quarkus.it.jpa.oracle;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test connecting Hibernate ORM to Oracle database.
 * Can quickly start a matching database with:
 * docker run -it --rm=true --name ORCLCDB -p 1521:1521 store/oracle/database-enterprise:12.2.0.1-slim
 */
@QuarkusTest
public class JPAFunctionalityTest {

    @Test
    public void testJPAFunctionalityFromServlet() throws Exception {
        RestAssured.when().get("/jpa-oracle/testfunctionality").then().body(is("OK"));
    }

}
