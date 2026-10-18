package io.quarkus.it.elasticsearch;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class FruitResourceTest {
    private static final TypeRef<List<Fruit>> LIST_OF_FRUIT_TYPE_REF = new TypeRef<>() {
    };

    @Test
    public void testEndpoint() {
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

        await().atMost(2, TimeUnit.SECONDS).pollDelay(500, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            // get the Fruit
            Fruit result = get("/fruits/1").as(Fruit.class);

            assertThat(result).isNotNull().isEqualTo(fruit);

            // search the Fruit
            List<Fruit> results = get("/fruits/search?color=Green").as(LIST_OF_FRUIT_TYPE_REF);
            assertThat(results).hasSize(1).contains(fruit);

            results = get("/fruits/search?name=Apple").as(LIST_OF_FRUIT_TYPE_REF);
            assertThat(results).hasSize(1).contains(fruit);
        });

        //create new fruit index via bulk operation
        Fruit pomegranate = new Fruit();
        pomegranate.id = "2";
        pomegranate.name = "Pomegranate";
        pomegranate.color = "Red";

        List<Fruit> fruits = List.of(pomegranate);

        given()
                .contentType("application/json")
                .body(fruits)
                .when().post("/fruits/bulk")
                .then()
                .statusCode(200);

        await().atMost(2, TimeUnit.SECONDS).pollDelay(500, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            Fruit result = get("/fruits/2").as(Fruit.class);
            assertThat(result).isNotNull().isEqualTo(pomegranate);
        });

        given()
                .contentType("application/json")
                .body(List.of(pomegranate.id))
                .when().delete("/fruits/bulk")
                .then()
                .statusCode(200);

        await().atMost(2, TimeUnit.SECONDS).pollDelay(500, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            // search the removed Fruit should yield no results
            // using a simple get-doc-by-id won't work since we will get an 500 error response:
            List<Fruit> results = get("/fruits/search?color=Red").as(LIST_OF_FRUIT_TYPE_REF);
            assertThat(results).isEmpty();
        });

    }
}
