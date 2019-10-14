package io.quarkus.it.jpa.derby;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test connecting Hibernate ORM to Derby.
 */
@QuarkusTest
public class JPAFunctionalityTest {

    @Test
    public void testJPAFunctionalityFromServlet() throws Exception {
        RestAssured.when().get("/jpa-derby/testfunctionality").then().body(is("OK"));
    }

}
