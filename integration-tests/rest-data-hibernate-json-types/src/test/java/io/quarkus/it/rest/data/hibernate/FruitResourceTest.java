package io.quarkus.it.rest.data.hibernate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class FruitResourceTest {

    @Test
    public void testPaginationDefaults() {
        given()
                .when().get("/fruits")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("content", hasSize(5))
                .body("content", hasSize(5))
                .body("totalElements", is(5))
                .body("hasNext", is(false))
                .body("hasPrevious", is(false));
    }

    @Test
    public void testPaginationWithSize() {
        given()
                .queryParam("size", 2)
                .when().get("/fruits")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("content", hasSize(2))
                .body("content", hasSize(2))
                .body("totalElements", is(5))
                .body("totalPages", is(3))
                .body("hasNext", is(true));
    }

    @Test
    public void testPaginationSecondPage() {
        given()
                .queryParam("page", 2)
                .queryParam("size", 2)
                .when().get("/fruits")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("content", hasSize(2))
                .body("hasPrevious", is(true))
                .body("hasNext", is(true));
    }

    @Test
    public void testSortAscending() {
        given()
                .queryParam("sort", "name")
                .queryParam("size", 3)
                .when().get("/fruits")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("content[0].name", is("Apple"))
                .body("content[1].name", is("Banana"))
                .body("content[2].name", is("Cherry"));
    }

    @Test
    public void testSortDescending() {
        given()
                .queryParam("sort", "-name")
                .queryParam("size", 3)
                .when().get("/fruits")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("content[0].name", is("Elderberry"))
                .body("content[1].name", is("Date"))
                .body("content[2].name", is("Cherry"));
    }
}
