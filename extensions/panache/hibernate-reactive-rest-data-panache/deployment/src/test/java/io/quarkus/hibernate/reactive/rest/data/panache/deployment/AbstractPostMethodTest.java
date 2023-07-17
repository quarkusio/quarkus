package io.quarkus.hibernate.reactive.rest.data.panache.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

public abstract class AbstractPostMethodTest {

    @Test
    void shouldCreateSimpleObject() {
        Response response = given().accept("application/json")
                .and().contentType("application/json")
                .and().body("{\"name\": \"test-simple\", \"collection\": {\"id\": \"full\"}}")
                .when().post("/items")
                .thenReturn();
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.header("Location")).isNotBlank();
        String id = response.header("Location").substring(response.header("Location").lastIndexOf("/") + 1);
        JsonPath body = response.body().jsonPath();
        assertThat(body.getString("id")).isEqualTo(id);
        assertThat(body.getString("name")).isEqualTo("test-simple");
    }

    @Test
    void shouldCreateSimpleHalObject() {
        Response response = given().accept("application/hal+json")
                .and().contentType("application/json")
                .and().body("{\"name\": \"test-simple-hal\", \"collection\": {\"id\": \"full\"}}")
                .when().post("/items")
                .thenReturn();
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.header("Location")).isNotBlank();
        String id = response.header("Location").substring(response.header("Location").lastIndexOf("/") + 1);
        JsonPath body = response.body().jsonPath();
        assertThat(body.getString("id")).isEqualTo(id);
        assertThat(body.getString("name")).isEqualTo("test-simple-hal");
        assertThat(body.getString("_links.add.href")).endsWith("/items");
        assertThat(body.getString("_links.list.href")).endsWith("/items");
        assertThat(body.getString("_links.self.href")).endsWith("/items/" + id);
        assertThat(body.getString("_links.update.href")).endsWith("/items/" + id);
        assertThat(body.getString("_links.remove.href")).endsWith("/items/" + id);
    }

    @Test
    void shouldCreateComplexObjects() {
        given().accept("application/json")
                .and().contentType("application/json")
                .and().body("{\"id\": \"test-complex\", \"name\": \"test collection\"}")
                .when().post("/collections")
                .then().statusCode(201)
                .and().header("Location", endsWith("/test-complex"))
                .and().body("id", is(equalTo("test-complex")))
                .and().body("name", is(equalTo("test collection")))
                .and().body("items", is(empty()));
        given().accept("application/json")
                .and().contentType("application/json")
                .and().body("{\"id\": \"test-complex\", \"name\": \"test collection\"}")
                .when().post("/collections")
                .then().statusCode(409);
    }

    @Test
    void shouldCreateComplexHalObjects() {
        given().accept("application/hal+json")
                .and().contentType("application/json")
                .and().body("{\"id\": \"test-complex-hal\", \"name\": \"test collection\"}")
                .when().post("/collections")
                .then().statusCode(201)
                .and().header("Location", endsWith("/test-complex-hal"))
                .and().body("id", is(equalTo("test-complex-hal")))
                .and().body("name", is(equalTo("test collection")))
                .and().body("items", is(empty()))
                .and().body("_links.add.href", endsWith("/collections"))
                .and().body("_links.list.href", endsWith("/collections"))
                .and().body("_links.self.href", endsWith("/collections/test-complex-hal"))
                .and().body("_links.update.href", endsWith("/collections/test-complex-hal"))
                .and().body("_links.remove.href", endsWith("/collections/test-complex-hal"));
        given().accept("application/hal+json")
                .and().contentType("application/json")
                .and().body("{\"id\": \"test-complex-hal\", \"name\": \"test collection\"}")
                .when().post("/collections")
                .then().statusCode(409);
    }
}
