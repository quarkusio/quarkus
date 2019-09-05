package io.quarkus.it.infinispan.client;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * @author William Burns
 */
@QuarkusTest
@QuarkusTestResource(InfinispanServerTestResource.class)
public class InfinispanClientFunctionalityTest {

    @Test
    public void testGetAllKeys() {
        System.out.println("Running getAllKeys test");
        RestAssured.when().get("/test").then().body(is("[book1, book2]"));
    }

    @Test
    public void testQuery() {
        System.out.println("Running query test");
        RestAssured.when().get("/test/query/So").then().body(is("[Son Martin]"));
        RestAssured.when().get("/test/query/org").then().body(is("[George Martin]"));
        RestAssured.when().get("/test/query/o").then().body(is("[George Martin,Son Martin]"));
    }

    @Test
    public void testIckleQuery() {
        System.out.println("Running ickleQuery test");
        RestAssured.when().get("/test/icklequery/So").then().body(is("[Son Martin]"));
        RestAssured.when().get("/test/icklequery/org").then().body(is("[George Martin]"));
        RestAssured.when().get("/test/icklequery/o").then().body(is("[George Martin,Son Martin]"));
    }

    @Test
    public void testCounterIncrement() {
        System.out.println("Running counterIncrement test");
        String initialValue = RestAssured.when().get("test/incr/somevalue").body().print();
        String nextValue = RestAssured.when().get("test/incr/somevalue").body().print();
        assertEquals(Integer.parseInt(initialValue) + 1, Integer.parseInt(nextValue));
    }

    @Test
    public void testCQ() {
        System.out.println("Running CQ test");
        RestAssured.when().get("/test/cq").then().body(is("2023"));
    }

    @Test
    public void testNearCacheInvalidation() {
        System.out.println("Running nearCacheInvalidation test");
        RestAssured.when().get("/test/nearcache").then().body(is("worked"));
    }

    @Test
    public void testQueryWithCustomMarshaller() {
        System.out.println("Running query with custom marshaller test");
        RestAssured.when().get("/test/magazinequery/IM").then().body(is("[TIME:1923-03,TIME:1997-04]"));
    }
}
