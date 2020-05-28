package io.quarkus.hibernate.orm.rest.data.panache.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

public abstract class AbstractGetMethodTest {

    @Test
    void shouldNotGetNonExistentObject() {
        given().accept("application/json")
                .when().get("/items/100")
                .then().statusCode(404);
    }

    @Test
    void shouldGetSimpleObject() {
        given().accept("application/json")
                .when().get("/items/1")
                .then().statusCode(200)
                .and().body("id", is(equalTo(1)))
                .and().body("name", is(equalTo("first")));
    }

    @Test
    void shouldGetSimpleHalObject() {
        given().accept("application/hal+json")
                .when().get("/items/1")
                .then().statusCode(200)
                .and().body("id", is(equalTo(1)))
                .and().body("name", is(equalTo("first")))
                .and().body("_links.add.href", endsWith("/items"))
                .and().body("_links.list.href", endsWith("/items"))
                .and().body("_links.self.href", endsWith("/items/1"))
                .and().body("_links.update.href", endsWith("/items/1"))
                .and().body("_links.remove.href", endsWith("/items/1"));
    }

    @Test
    void shouldGetComplexObjects() {
        given().accept("application/json")
                .when().get("/collections/full")
                .then().statusCode(200)
                .and().body("name", is(equalTo("full")))
                .and().body("items.id", contains(1, 2))
                .and().body("items.name", contains("first", "second"));
    }

    @Test
    void shouldGetComplexHalObjects() {
        given().accept("application/hal+json")
                .when().get("/collections/full")
                .then().statusCode(200)
                .and().body("name", is(equalTo("full")))
                .and().body("items.id", contains(1, 2))
                .and().body("items.name", contains("first", "second"))
                .and().body("_links.add.href", endsWith("/collections"))
                .and().body("_links.list.href", endsWith("/collections"))
                .and().body("_links.self.href", endsWith("/collections/full"))
                .and().body("_links.update.href", endsWith("/collections/full"))
                .and().body("_links.remove.href", endsWith("/collections/full"));
    }

    @Test
    void shouldListSimpleObjects() {
        given().accept("application/json")
                .when().get("/items")
                .then().statusCode(200)
                .and().body("id", contains(1, 2))
                .and().body("name", contains("first", "second"));
    }

    @Test
    void shouldListSimpleHalObjects() {
        given().accept("application/hal+json")
                .when().get("/items")
                .then().statusCode(200)
                .and().body("_embedded.items.id", contains(1, 2))
                .and().body("_embedded.items.name", contains("first", "second"))
                .and().body("_embedded.items._links.add.href", contains(endsWith("/items"), endsWith("/items")))
                .and().body("_embedded.items._links.list.href", contains(endsWith("/items"), endsWith("/items")))
                .and().body("_embedded.items._links.self.href", contains(endsWith("/items/1"), endsWith("/items/2")))
                .and().body("_embedded.items._links.update.href", contains(endsWith("/items/1"), endsWith("/items/2")))
                .and().body("_embedded.items._links.remove.href", contains(endsWith("/items/1"), endsWith("/items/2")))
                .and().body("_links.add.href", endsWith("/items"))
                .and().body("_links.list.href", endsWith("/items"));
    }

    @Test
    void shouldListComplexObjects() {
        given().accept("application/json")
                .when().get("/collections")
                .then().statusCode(200)
                .and().body("name", contains("empty", "full"))
                .and().body("items.id[0]", is(empty()))
                .and().body("items.id[1]", contains(1, 2))
                .and().body("items.name[1]", contains("first", "second"));
    }

    @Test
    void shouldListComplexHalObjects() {
        given().accept("application/hal+json")
                .when().get("/collections")
                .then().statusCode(200)
                .and().body("_embedded.collections.name", contains("empty", "full"))
                .and().body("_embedded.collections.items.id[0]", is(empty()))
                .and().body("_embedded.collections.items.id[1]", contains(1, 2))
                .and().body("_embedded.collections.items.name[1]", contains("first", "second"))
                .and()
                .body("_embedded.collections._links.add.href", contains(endsWith("/collections"), endsWith("/collections")))
                .and()
                .body("_embedded.collections._links.list.href", contains(endsWith("/collections"), endsWith("/collections")))
                .and()
                .body("_embedded.collections._links.self.href",
                        contains(endsWith("/collections/empty"), endsWith("/collections/full")))
                .and()
                .body("_embedded.collections._links.update.href",
                        contains(endsWith("/collections/empty"), endsWith("/collections/full")))
                .and()
                .body("_embedded.collections._links.remove.href",
                        contains(endsWith("/collections/empty"), endsWith("/collections/full")))
                .and().body("_links.add.href", endsWith("/collections"))
                .and().body("_links.list.href", endsWith("/collections"));
    }
}
