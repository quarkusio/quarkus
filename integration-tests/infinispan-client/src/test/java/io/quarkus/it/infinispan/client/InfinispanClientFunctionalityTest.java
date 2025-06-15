package io.quarkus.it.infinispan.client;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.infinispan.counter.api.CounterType;
import org.infinispan.counter.api.Storage;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class InfinispanClientFunctionalityTest {

    @Test
    public void testGetAllKeys() {
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
    public void testCounterIncrement() {
        RestAssured.given().queryParam("type", CounterType.BOUNDED_STRONG).queryParam("storage", Storage.VOLATILE)
                .post("test/counter/strong-1").body().print();

        RestAssured.given().queryParam("type", CounterType.WEAK).queryParam("storage", Storage.VOLATILE)
                .post("test/counter/weak-1").body().print();

        RestAssured.given().queryParam("type", CounterType.UNBOUNDED_STRONG).queryParam("storage", Storage.PERSISTENT)
                .post("test/counter/strong-2").body().print();

        assertCounterIncrement("strong-1");
        assertCounterIncrement("weak-1");
        assertCounterIncrement("strong-2");
    }

    private void assertCounterIncrement(String counterName) {
        String initialValue = RestAssured.given().get("test/incr/" + counterName).body().print();
        String nextValue = RestAssured.when().get("test/incr/" + counterName).body().print();
        assertEquals(Integer.parseInt(initialValue) + 1, Integer.parseInt(nextValue));
    }

    @Test
    public void testCQ() {
        RestAssured.when().get("/test/cq").then().body(is("2023"));
    }

    @Test
    public void testNearCacheInvalidation() {
        RestAssured.when().get("/test/nearcache").then().body(is("worked"));
    }

    @Test
    public void testQueryWithCustomMarshaller() {
        RestAssured.when().get("/test/magazinequery/IM").then().body(is("[TIME:1923-03,TIME:1997-04]"));
    }

    @Test
    public void testAuthor() {
        RestAssured.when().get("/test/create-cache-default-config/authors").then()
                .body(is("[George,J. K. Rowling,Son]"));
    }

    @Test
    public void testCacheAnnotations() {
        RestAssured.when().get("/books/hp-1").then().body(containsString("Philosopher's Stone"));

        RestAssured.when().get("/books/hp-2").then().body(containsString("Chamber of Secrets"));

        RestAssured.when().get("/books/hp-3").then().body(containsString("Prisoner of Azkaban"));

        RestAssured.when().get("/books/hp-4").then().body(containsString("computed book"));

        RestAssured.when().get("/books/hp-3/extra-params").then().statusCode(500);

        RestAssured.when().delete("/books/hp-1").then().statusCode(200);

        RestAssured.when().get("/books/hp-1").then().body(containsString("computed book"));

        RestAssured.when().delete("/books").then().statusCode(200);

        RestAssured.when().get("/books/hp-2").then().body(containsString("computed book"));

        RestAssured.when().get("/books/hp-3").then().body(containsString("computed book"));

    }
}
