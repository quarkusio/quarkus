package io.quarkus.it.jpa.oracle;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test connecting Hibernate ORM to Oracle database.
 * Can quickly start a matching database with:
 *
 * <pre>
 * docker run --rm=true --name=HibernateTestingOracle -p 1521:1521 -e ORACLE_PASSWORD=hibernate_orm_test docker.io/gvenzl/oracle-free:23-slim-faststart
 * </pre>
 */
@QuarkusTest
public class JPAFunctionalityTest {

    @Test
    public void testJPAFunctionalityFromServlet() throws Exception {
        RestAssured.when().get("/jpa-oracle/testfunctionality").then().body(is("OK"));
    }

    @Test
    public void testSerializationFromServlet() throws Exception {
        RestAssured.when().get("/jpa-oracle/testserialization").then().body(is("Hello from Serialization Test"));
    }

    @Test
    public void testLdapFromServlet() throws Exception {
        RestAssured.when().get("/jpa-oracle/testldap").then().body(is("OK"));
    }

}
