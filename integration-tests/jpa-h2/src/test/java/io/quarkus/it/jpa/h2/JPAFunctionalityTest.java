package io.quarkus.it.jpa.h2;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test connecting Hibernate ORM to H2.
 * The H2 database server is run in JVM mode, the Hibernate based application
 * is run in both JVM mode and native mode (see also test in subclass).
 */
@QuarkusTest
public class JPAFunctionalityTest {

    @Test
    public void testJPAFunctionalityFromServlet() throws Exception {
        RestAssured.when().get("/jpa-h2/testfunctionality").then().body(is("OK"));
    }

    @Test
    public void testHibernateEnhancedProxies() throws Exception {
        RestAssured.when().get("/jpa-h2/testproxy").then().body(is("OK"));
    }

    @Test
    public void testHibernateEnhancedBasicProxies() throws Exception {
        RestAssured.when().get("/jpa-h2/testbasicproxy").then().body(is("OK"));
    }

}
