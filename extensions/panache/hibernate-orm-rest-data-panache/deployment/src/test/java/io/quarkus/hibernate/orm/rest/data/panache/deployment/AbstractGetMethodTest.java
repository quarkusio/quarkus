package io.quarkus.hibernate.orm.rest.data.panache.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.Link;

import org.junit.jupiter.api.Test;

import io.restassured.http.Header;
import io.restassured.response.Response;

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
    void shouldGetComplexObject() {
        given().accept("application/json")
                .when().get("/collections/full")
                .then().statusCode(200)
                .and().body("id", is(equalTo("full")))
                .and().body("name", is(equalTo("full collection")))
                .and().body("items.id", contains(1, 2))
                .and().body("items.name", contains("first", "second"));
    }

    @Test
    void shouldGetComplexHalObject() {
        given().accept("application/hal+json")
                .when().get("/collections/full")
                .then().statusCode(200)
                .and().body("id", is(equalTo("full")))
                .and().body("name", is(equalTo("full collection")))
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
    void shouldListWithFilter() {
        given().accept("application/json")
                .when()
                .queryParam("name", "first")
                .get("/items")
                .then().statusCode(200)
                .and().body("id", contains(1))
                .and().body("name", contains("first"));
    }

    @Test
    void shouldListWithManyFilters() {
        given().accept("application/json")
                .when()
                .queryParam("id", 1)
                .queryParam("name", "first")
                .get("/items")
                .then().statusCode(200)
                .and().body("id", contains(1))
                .and().body("name", contains("first"));
    }

    @Test
    void shouldListWithNamedQuery() {
        given().accept("application/json")
                .when()
                .queryParam("name", "s")
                .queryParam("namedQuery", "Item.containsInName")
                .get("/items")
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
    void shouldListSimpleAscendingObjects() {
        given().accept("application/json")
                .when().get("/items?sort=name,id")
                .then().statusCode(200)
                .and().body("id", contains(1, 2))
                .and().body("name", contains("first", "second"));
    }

    @Test
    void shouldListSimpleDescendingObjects() {
        given().accept("application/json")
                .when().get("/items?sort=-name,id")
                .then().statusCode(200)
                .and().body("id", contains(2, 1))
                .and().body("name", contains("second", "first"));
    }

    @Test
    void shouldListSimpleDescendingObjectsAndFilter() {
        given().accept("application/json")
                .when()
                .queryParam("name", "first")
                .get("/items?sort=-name,id")
                .then().statusCode(200)
                .and().body("id", contains(1))
                .and().body("name", contains("first"));
    }

    @Test
    void shouldNotListWithInvalidSortParam() {
        given().accept("application/json")
                .when().get("/items?sort=1name")
                .then().statusCode(400)
                .and().body(is(equalTo("Invalid sort parameter '1name'")));
    }

    @Test
    void shouldNotListHalWithInvalidSortParam() {
        given().accept("application/hal+json")
                .when().get("/items?sort=1name")
                .then().statusCode(400)
                .and().body(is(equalTo("Invalid sort parameter '1name'")));
    }

    @Test
    void shouldListComplexObjects() {
        given().accept("application/json")
                .when().get("/collections")
                .then().statusCode(200)
                .and().body("id", contains("empty", "full"))
                .and().body("name", contains("empty collection", "full collection"))
                .and().body("items.id[0]", is(empty()))
                .and().body("items.id[1]", contains(1, 2))
                .and().body("items.name[1]", contains("first", "second"));
    }

    @Test
    void shouldListComplexHalObjects() {
        given().accept("application/hal+json")
                .when().get("/collections")
                .then().statusCode(200)
                .and().body("_embedded.item-collections.id", contains("empty", "full"))
                .and().body("_embedded.item-collections.name", contains("empty collection", "full collection"))
                .and().body("_embedded.item-collections.items.id[0]", is(empty()))
                .and().body("_embedded.item-collections.items.id[1]", contains(1, 2))
                .and().body("_embedded.item-collections.items.name[1]", contains("first", "second"))
                .and()
                .body("_embedded.item-collections._links.add.href",
                        contains(endsWith("/collections"), endsWith("/collections")))
                .and()
                .body("_embedded.item-collections._links.list.href",
                        contains(endsWith("/collections"), endsWith("/collections")))
                .and()
                .body("_embedded.item-collections._links.self.href",
                        contains(endsWith("/collections/empty"), endsWith("/collections/full")))
                .and()
                .body("_embedded.item-collections._links.update.href",
                        contains(endsWith("/collections/empty"), endsWith("/collections/full")))
                .and()
                .body("_embedded.item-collections._links.remove.href",
                        contains(endsWith("/collections/empty"), endsWith("/collections/full")))
                .and().body("_links.add.href", endsWith("/collections"))
                .and().body("_links.list.href", endsWith("/collections"));
    }

    @Test
    void shouldNotGetNonExistentPage() {
        given().accept("application/json")
                .and().queryParam("page", 100)
                .when().get("/items")
                .then().statusCode(200)
                .and().body("id", is(empty()));
    }

    @Test
    void shouldNotGetNegativePageOrSize() {
        given().accept("application/json")
                .and().queryParam("page", -1)
                .and().queryParam("size", -1)
                .when().get("/items")
                .then().statusCode(200)
                // Invalid page and size parameters are replaced with defaults
                .and().body("id", contains(1, 2));
    }

    @Test
    void shouldGetFirstPage() {
        Response response = given().accept("application/json")
                .and().queryParam("page", 0)
                .and().queryParam("size", 1)
                .when().get("/items")
                .thenReturn();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.body().jsonPath().getList("id")).containsOnly(1);
        assertThat(response.body().jsonPath().getList("name")).containsOnly("first");

        List<Link> links = new LinkedList<>();
        for (Header header : response.getHeaders().getList("Link")) {
            links.add(Link.valueOf(header.getValue()));
        }
        assertThat(links).hasSize(3);
        assertThat(links).anySatisfy(link -> {
            assertThat(link.getUri().toString()).endsWith("/items?page=0&size=1");
            assertThat(link.getRel()).isEqualTo("first");
        });
        assertThat(links).anySatisfy(link -> {
            assertThat(link.getUri().toString()).endsWith("/items?page=1&size=1");
            assertThat(link.getRel()).isEqualTo("last");
        });
        assertThat(links).anySatisfy(link -> {
            assertThat(link.getUri().toString()).endsWith("/items?page=1&size=1");
            assertThat(link.getRel()).isEqualTo("next");
        });
    }

    @Test
    void shouldGetFirstPageWithFilter() {
        Response response = given().accept("application/json")
                .and().queryParam("page", 0)
                .and().queryParam("size", 1)
                .and().queryParam("name", "second")
                .when().get("/items")
                .thenReturn();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.body().jsonPath().getList("id")).containsOnly(2);
        assertThat(response.body().jsonPath().getList("name")).containsOnly("second");
    }

    @Test
    void shouldGetFirstHalPage() {
        Response response = given().accept("application/hal+json")
                .and().queryParam("page", 0)
                .and().queryParam("size", 1)
                .when().get("/items")
                .thenReturn();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.body().jsonPath().getList("_embedded.items.id")).containsOnly(1);
        assertThat(response.body().jsonPath().getList("_embedded.items.name")).containsOnly("first");
        assertThat(response.body().jsonPath().getString("_links.add.href")).endsWith("/items");
        assertThat(response.body().jsonPath().getString("_links.list.href")).endsWith("/items");
        assertThat(response.body().jsonPath().getString("_links.first.href")).endsWith("/items?page=0&size=1");
        assertThat(response.body().jsonPath().getString("_links.last.href")).endsWith("/items?page=1&size=1");
        assertThat(response.body().jsonPath().getString("_links.previous.href")).isNull();
        assertThat(response.body().jsonPath().getString("_links.next.href")).endsWith("/items?page=1&size=1");

        List<Link> links = new LinkedList<>();
        for (Header header : response.getHeaders().getList("Link")) {
            links.add(Link.valueOf(header.getValue()));
        }
        assertThat(links).hasSize(3);
        assertThat(links).anySatisfy(link -> {
            assertThat(link.getUri().toString()).endsWith("/items?page=0&size=1");
            assertThat(link.getRel()).isEqualTo("first");
        });
        assertThat(links).anySatisfy(link -> {
            assertThat(link.getUri().toString()).endsWith("/items?page=1&size=1");
            assertThat(link.getRel()).isEqualTo("last");
        });
        assertThat(links).anySatisfy(link -> {
            assertThat(link.getUri().toString()).endsWith("/items?page=1&size=1");
            assertThat(link.getRel()).isEqualTo("next");
        });
    }

    @Test
    void shouldGetLastPage() {
        Response response = given().accept("application/json")
                .and().queryParam("page", 1)
                .and().queryParam("size", 1)
                .when().get("/items")
                .thenReturn();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.body().jsonPath().getList("id")).containsOnly(2);
        assertThat(response.body().jsonPath().getList("name")).containsOnly("second");

        List<Link> links = new LinkedList<>();
        for (Header header : response.getHeaders().getList("Link")) {
            links.add(Link.valueOf(header.getValue()));
        }
        assertThat(links).hasSize(3);
        assertThat(links).anySatisfy(link -> {
            assertThat(link.getUri().toString()).endsWith("/items?page=0&size=1");
            assertThat(link.getRel()).isEqualTo("first");
        });
        assertThat(links).anySatisfy(link -> {
            assertThat(link.getUri().toString()).endsWith("/items?page=1&size=1");
            assertThat(link.getRel()).isEqualTo("last");
        });
        assertThat(links).anySatisfy(link -> {
            assertThat(link.getUri().toString()).endsWith("/items?page=0&size=1");
            assertThat(link.getRel()).isEqualTo("previous");
        });
    }

    @Test
    void shouldGetLastHalPage() {
        Response response = given().accept("application/hal+json")
                .and().queryParam("page", 1)
                .and().queryParam("size", 1)
                .when().get("/items")
                .thenReturn();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.body().jsonPath().getList("_embedded.items.id")).containsOnly(2);
        assertThat(response.body().jsonPath().getList("_embedded.items.name")).containsOnly("second");
        assertThat(response.body().jsonPath().getString("_links.add.href")).endsWith("/items");
        assertThat(response.body().jsonPath().getString("_links.list.href")).endsWith("/items");
        assertThat(response.body().jsonPath().getString("_links.first.href")).endsWith("/items?page=0&size=1");
        assertThat(response.body().jsonPath().getString("_links.last.href")).endsWith("/items?page=1&size=1");
        assertThat(response.body().jsonPath().getString("_links.previous.href")).endsWith("/items?page=0&size=1");
        assertThat(response.body().jsonPath().getString("_links.next.href")).isNull();

        List<Link> links = new LinkedList<>();
        for (Header header : response.getHeaders().getList("Link")) {
            links.add(Link.valueOf(header.getValue()));
        }
        assertThat(links).hasSize(3);
        assertThat(links).anySatisfy(link -> {
            assertThat(link.getUri().toString()).endsWith("/items?page=0&size=1");
            assertThat(link.getRel()).isEqualTo("first");
        });
        assertThat(links).anySatisfy(link -> {
            assertThat(link.getUri().toString()).endsWith("/items?page=1&size=1");
            assertThat(link.getRel()).isEqualTo("last");
        });
        assertThat(links).anySatisfy(link -> {
            assertThat(link.getUri().toString()).endsWith("/items?page=0&size=1");
            assertThat(link.getRel()).isEqualTo("previous");
        });
    }

    @Test
    void shouldListEmptyTables() {
        given().accept("application/hal+json")
                .and().queryParam("page", 1)
                .and().queryParam("size", 1)
                .when().get("/empty-list-items")
                .then().statusCode(200);
    }
}
