package io.quarkus.hibernate.orm.rest.data.panache.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

public abstract class AbstractPatchMethodTest {

    @Test
    void shouldUpdateSimpleObject() {
        given().accept("application/json")
                .and().contentType("application/json")
                .and().body("{\"id\": \"1\", \"name\": \"first-test\", \"collection\": {\"id\": \"full\"}}")
                .when().patch("/items/1")
                .then().statusCode(204);
        given().accept("application/json")
                .when().get("/items/1")
                .then().statusCode(200)
                .and().body("id", is(equalTo(1)))
                .and().body("name", is(equalTo("first-test")));
    }

    @Test
    void shouldUpdateComplexObject() {
        given().accept("application/json")
                .and().contentType("application/json")
                .and().body("{\"name\": \"updated collection\"}")
                .when().patch("/collections/empty")
                .then().statusCode(204);
        given().accept("application/json")
                .when().get("/collections/empty")
                .then().statusCode(200)
                .and().body("id", is(equalTo("empty")))
                .and().body("name", is(equalTo("updated collection")))
                .and().body("age", is(equalTo(13)));
    }

    @Test
    void shouldNotUpdateSimpleObjectId() {
        given().accept("application/json")
                .and().contentType("application/json")
                .and().body("{\"id\": \"100\", \"name\": \"second\", \"collection\": {\"id\": \"full\"}}")
                .when().patch("/items/2")
                .then().statusCode(204);
        given().accept("application/json")
                .when().get("/items/2")
                .then().statusCode(200)
                .and().body("id", is(equalTo(2)))
                .and().body("name", is(equalTo("second")));
        given().accept("application/json")
                .when().get("/items/100")
                .then().statusCode(404);
    }

    @Test
    void shouldNotUpdateComplexObjectId() {
        given().accept("application/json")
                .and().contentType("application/json")
                .and().body("{\"id\": \"updated-empty\", \"name\": \"empty collection\"}")
                .when().patch("/collections/empty")
                .then().statusCode(204);
        given().accept("application/json")
                .when().get("/collections/empty")
                .then().statusCode(200)
                .and().body("id", is(equalTo("empty")))
                .and().body("name", is(equalTo("empty collection")));
        given().accept("application/json")
                .when().get("/collections/updated-empty")
                .then().statusCode(404);
    }

    @Test
    void shouldCreateHalObjectWithRequiredId() {
        given().accept("application/hal+json")
                .and().contentType("application/json")
                .and().body("{\"id\": \"test-collection-hal\", \"name\": \"test collection\"}")
                .when().patch("/collections/test-collection-hal")
                .then().statusCode(201)
                .and().header("Location", endsWith("/collections/test-collection-hal"))
                .and().body("id", is(equalTo("test-collection-hal")))
                .and().body("name", is(equalTo("test collection")))
                .and().body("items", is(empty()))
                .and().body("_links.add.href", endsWith("/collections"))
                .and().body("_links.list.href", endsWith("/collections"))
                .and().body("_links.self.href", endsWith("/collections/test-collection-hal"))
                .and().body("_links.update.href", endsWith("/collections/test-collection-hal"))
                .and().body("_links.remove.href", endsWith("/collections/test-collection-hal"));
    }

    @Test
    void shouldCreateHalObjectWithGeneratedId() {
        Response response = given().accept("application/hal+json")
                .and().contentType("application/json")
                .and().body("{\"name\": \"test-item-hal\", \"collection\": {\"id\": \"full\"}}")
                .when().patch("/items/12")
                .thenReturn();
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.header("Location")).isNotBlank();
        String id = response.header("Location").substring(response.header("Location").lastIndexOf("/") + 1);
        JsonPath body = response.body().jsonPath();
        assertThat(body.getString("id")).isEqualTo(id);
        assertThat(body.getString("name")).isEqualTo("test-item-hal");
        assertThat(body.getString("_links.add.href")).endsWith("/items");
        assertThat(body.getString("_links.list.href")).endsWith("/items");
        assertThat(body.getString("_links.self.href")).endsWith("/items/" + id);
        assertThat(body.getString("_links.update.href")).endsWith("/items/" + id);
        assertThat(body.getString("_links.remove.href")).endsWith("/items/" + id);
    }

}
