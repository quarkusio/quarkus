package io.quarkus.it.elasticsearch;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.it.elasticsearch.java.Fruit;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class FruitResourceTest {
    private static final TypeRef<List<Fruit>> LIST_OF_FRUIT_TYPE_REF = new TypeRef<List<Fruit>>() {
    };

    @Test
    public void testEndpoint() throws InterruptedException {
        // create a Fruit
        Fruit fruit = new Fruit();
        fruit.id = "1";
        fruit.name = "Apple";
        fruit.color = "Green";
        given()
                .contentType("application/json")
                .body(fruit)
                .when().post("/fruits")
                .then()
                .statusCode(201);

        // get the Fruit
        Fruit result = get("/fruits/1").as(Fruit.class);
        assertNotNull(result);
        assertEquals("1", result.id);
        assertEquals("Apple", result.name);
        assertEquals("Green", result.color);

        // wait a few ms for the indexing to happened
        Thread.sleep(1000);

        // search the Fruit
        List<Fruit> results = get("/fruits/search?color=Green").as(LIST_OF_FRUIT_TYPE_REF);
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals("1", results.get(0).id);
        assertEquals("Apple", results.get(0).name);
        assertEquals("Green", results.get(0).color);

        results = get("/fruits/search?name=Apple").as(LIST_OF_FRUIT_TYPE_REF);
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals("1", results.get(0).id);
        assertEquals("Apple", results.get(0).name);
        assertEquals("Green", results.get(0).color);

        results = RestAssured.given().queryParam("json",
                "{\n" +
                        "\"query\": {\n" +
                        "    \"prefix\": {\n" +
                        "        \"name\": {\n" +
                        "            \"value\": \"app\"\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n" +
                        "}\n")
                .get("/fruits/search/unsafe").as(LIST_OF_FRUIT_TYPE_REF);
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals("1", results.get(0).id);
        assertEquals("Apple", results.get(0).name);
        assertEquals("Green", results.get(0).color);
    }

    @Test
    public void testHealth() {
        RestAssured.when().get("/q/health/ready").then()
                .body("status", is("UP"),
                        "checks.status", containsInAnyOrder("UP"),
                        "checks.name", containsInAnyOrder("Elasticsearch cluster health check"));
    }

}
