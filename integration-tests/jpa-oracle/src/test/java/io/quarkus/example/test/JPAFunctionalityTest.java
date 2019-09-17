package io.quarkus.example.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Test connecting Hibernate ORM to Oracle database.
 */
@QuarkusTest
public class JPAFunctionalityTest {

    @Test
    public void testJPAFunctionalityFromServlet() throws Exception {
        when().get("/jpa-oracle/testfunctionality").then().body(is("OK"));
    }

}
