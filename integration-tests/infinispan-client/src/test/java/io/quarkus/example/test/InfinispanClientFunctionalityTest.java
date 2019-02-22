package io.quarkus.example.test;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * @author William Burns
 */
@QuarkusTest
public class InfinispanClientFunctionalityTest {
    @Test
    public void testClientFunctionalityFromServlet() {
        RestAssured.when().get("/test").then().body(is("[book1, book2]"));
    }

    @Test
    public void testQuery() {
        RestAssured.when().get("/test/query/So").then().body(is("[Son Martin]"));
        RestAssured.when().get("/test/query/org").then().body(is("[George Martin]"));
        RestAssured.when().get("/test/query/o").then().body(is("[George Martin,Son Martin]"));
    }

    @Test
    public void testIckleQuery() {
        RestAssured.when().get("/test/icklequery/So").then().body(is("[Son Martin]"));
        RestAssured.when().get("/test/icklequery/org").then().body(is("[George Martin]"));
        RestAssured.when().get("/test/icklequery/o").then().body(is("[George Martin,Son Martin]"));
    }

    @Test
    public void increment() {
        String initialValue = RestAssured.when().get("test/incr/somevalue").body().print();
        String nextValue = RestAssured.when().get("test/incr/somevalue").body().print();
        assertEquals(Integer.parseInt(initialValue) + 1, Integer.parseInt(nextValue));
    }

    @Test
    public void cq() {
        RestAssured.when().get("/test/cq").then().body(is("2023"));
    }

    @Test
    public void testNearCacheInvalidation() {
        RestAssured.when().get("/test/nearcache").then().body(is("worked"));
    }
}
