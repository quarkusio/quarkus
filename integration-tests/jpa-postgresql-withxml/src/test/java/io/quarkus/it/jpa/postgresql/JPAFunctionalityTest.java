package io.quarkus.it.jpa.postgresql;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test various JPA operations running in Quarkus
 */
@QuarkusTest
public class JPAFunctionalityTest {

    @Test
    public void testBase() throws Exception {
        RestAssured.when().get("/jpa-withxml/testfunctionality/base").then().body(is("OK"));
    }

    @Test
    public void testDatasourceXml() throws Exception {
        RestAssured.when().get("/jpa-withxml/testfunctionality/datasource-xml").then().body(is("OK"));
    }

    @Test
    public void testHibernateXml() throws Exception {
        RestAssured.when().get("/jpa-withxml/testfunctionality/hibernate-xml").then().body(is("OK"));
    }

}
