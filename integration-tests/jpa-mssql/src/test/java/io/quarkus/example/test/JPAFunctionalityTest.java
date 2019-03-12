package io.quarkus.example.test;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test connecting Hibernate ORM to MS SQL.
 * Can quickly start a matching database with:
 * sudo docker run --ulimit memlock=-1:-1 -it --rm=true --memory-swappiness=0 -e 'ACCEPT_EULA=Y' -e
 * 'SA_PASSWORD=ActuallyRequired11Complexity' -p 1433:1433 --name quarkus_test_mssql -d microsoft/mssql-server-linux:2017-CU12
 */
@QuarkusTest
public class JPAFunctionalityTest {

    @Test
    public void testJPAFunctionalityFromServlet() throws Exception {
        RestAssured.when().get("/jpa-mssql/testfunctionality").then().body(is("OK"));
    }

}
