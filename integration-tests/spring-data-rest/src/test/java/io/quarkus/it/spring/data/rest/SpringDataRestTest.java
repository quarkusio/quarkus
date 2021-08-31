package io.quarkus.it.spring.data.rest;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

@QuarkusTest
class SpringDataRestTest {

    private static final int DOSTOEVSKY_ID = 1;

    private static final String DOSTOEVSKY_NAME = "Fyodor Dostoevsky";

    private static final String DOSTOEVSKY_DOB = "1821-11-11";

    private static final int CRIME_AND_PUNISHMENT_ID = 2;

    private static final String CRIME_AND_PUNISHMENT_TITLE = "Crime and Punishment";

    private static final int IDIOT_ID = 3;

    private static final String IDIOT_TITLE = "Idiot";

    @Test
    void shouldGetAuthor() {
        given().accept("application/json")
                .when().get("/authors/" + DOSTOEVSKY_ID)
                .then().statusCode(200)
                .and().body("id", is(equalTo(DOSTOEVSKY_ID)))
                .and().body("name", is(equalTo(DOSTOEVSKY_NAME)))
                .and().body("dob", is(equalTo(DOSTOEVSKY_DOB)));
    }

    @Test
    void shouldGetBook() {
        given().accept("application/json")
                .when().get("/books/" + CRIME_AND_PUNISHMENT_ID)
                .then().statusCode(200)
                .and().body("id", is(equalTo(CRIME_AND_PUNISHMENT_ID)))
                .and().body("title", is(equalTo(CRIME_AND_PUNISHMENT_TITLE)))
                .and().body("author.id", is(equalTo(DOSTOEVSKY_ID)))
                .and().body("author.name", is(equalTo(DOSTOEVSKY_NAME)))
                .and().body("author.dob", is(equalTo(DOSTOEVSKY_DOB)));
    }

    @Test
    void shouldGetBookHal() {
        given().accept("application/hal+json")
                .when().get("/books/" + CRIME_AND_PUNISHMENT_ID)
                .then().statusCode(200)
                .and().body("id", is(equalTo(CRIME_AND_PUNISHMENT_ID)))
                .and().body("title", is(equalTo(CRIME_AND_PUNISHMENT_TITLE)))
                .and().body("author.id", is(equalTo(DOSTOEVSKY_ID)))
                .and().body("author.name", is(equalTo(DOSTOEVSKY_NAME)))
                .and().body("author.dob", is(equalTo(DOSTOEVSKY_DOB)))
                .and().body("_links.add.href", endsWith("/books"))
                .and().body("_links.list.href", endsWith("/books"))
                .and().body("_links.self.href", endsWith("/books/" + CRIME_AND_PUNISHMENT_ID))
                .and().body("_links.update.href", endsWith("/books/" + CRIME_AND_PUNISHMENT_ID))
                .and().body("_links.remove.href", endsWith("/books/" + CRIME_AND_PUNISHMENT_ID));
    }

    @Test
    void shouldListAuthors() {
        given().accept("application/json")
                .when().get("/authors")
                .then().statusCode(200)
                .and().body("id", contains(DOSTOEVSKY_ID))
                .and().body("name", contains(DOSTOEVSKY_NAME))
                .and().body("dob", contains(DOSTOEVSKY_DOB));
    }

    @Test
    void shouldListBooks() {
        given().accept("application/json")
                .when().get("/books")
                .then().statusCode(200)
                .and().body("id", contains(CRIME_AND_PUNISHMENT_ID, IDIOT_ID))
                .and().body("title", contains(CRIME_AND_PUNISHMENT_TITLE, IDIOT_TITLE))
                .and().body("author.id", contains(DOSTOEVSKY_ID, DOSTOEVSKY_ID))
                .and().body("author.name", contains(DOSTOEVSKY_NAME, DOSTOEVSKY_NAME))
                .and().body("author.dob", contains(DOSTOEVSKY_DOB, DOSTOEVSKY_DOB));
    }

    @Test
    void shouldListBooksHal() {
        given().accept("application/hal+json")
                .when().get("/books")
                .then().statusCode(200)
                .and().body("_embedded.books.id", contains(CRIME_AND_PUNISHMENT_ID, IDIOT_ID))
                .and().body("_embedded.books.title", contains(CRIME_AND_PUNISHMENT_TITLE, IDIOT_TITLE))
                .and().body("_embedded.books.author.id", contains(DOSTOEVSKY_ID, DOSTOEVSKY_ID))
                .and().body("_embedded.books.author.name", contains(DOSTOEVSKY_NAME, DOSTOEVSKY_NAME))
                .and().body("_embedded.books.author.dob", contains(DOSTOEVSKY_DOB, DOSTOEVSKY_DOB))
                .and().body("_embedded.books._links.add.href", contains(endsWith("/books"), endsWith("/books")))
                .and().body("_embedded.books._links.list.href", contains(endsWith("/books"), endsWith("/books")))
                .and().body("_embedded.books._links.self.href",
                        contains(endsWith("/books/" + CRIME_AND_PUNISHMENT_ID), endsWith("/books/" + IDIOT_ID)))
                .and().body("_embedded.books._links.update.href",
                        contains(endsWith("/books/" + CRIME_AND_PUNISHMENT_ID), endsWith("/books/" + IDIOT_ID)))
                .and().body("_embedded.books._links.remove.href",
                        contains(endsWith("/books/" + CRIME_AND_PUNISHMENT_ID), endsWith("/books/" + IDIOT_ID)))
                .and().body("_links.add.href", endsWith("/books"))
                .and().body("_links.list.href", endsWith("/books"));
    }

    @Test
    void shouldNotCreateOrDeleteAuthor() {
        JsonObject author = Json.createObjectBuilder()
                .add("name", "test")
                .add("dob", "1900-01-01")
                .build();
        given().contentType("application/json")
                .and().body(author.toString())
                .when().post("/authors")
                .then().statusCode(405);
        when().delete("/authors/" + DOSTOEVSKY_ID)
                .then().statusCode(405);
    }

    @Test
    void shouldCreateAndDeleteBook() {
        JsonObject author = Json.createObjectBuilder()
                .add("id", DOSTOEVSKY_ID)
                .add("name", DOSTOEVSKY_NAME)
                .add("dob", DOSTOEVSKY_DOB)
                .build();
        JsonObject book = Json.createObjectBuilder()
                .add("title", "The Brothers Karamazov")
                .add("author", author)
                .build();
        Response response = given().accept("application/json")
                .and().contentType("application/json")
                .and().body(book.toString())
                .when().post("/books")
                .thenReturn();
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.header("Location")).isNotEmpty();

        when().delete(response.getHeader("Location"))
                .then().statusCode(204);
        given().accept("application/json")
                .when().get(response.getHeader("Location"))
                .then().statusCode(404);
    }

    @Test
    void shouldNotCreateBookWithBlankTitle() {
        JsonObject author = Json.createObjectBuilder()
                .add("id", DOSTOEVSKY_ID)
                .add("name", DOSTOEVSKY_NAME)
                .add("dob", DOSTOEVSKY_DOB)
                .build();
        JsonObject book = Json.createObjectBuilder()
                .add("title", "")
                .add("author", author)
                .build();
        given().accept("application/json")
                .and().contentType("application/json")
                .and().body(book.toString())
                .when().post("/books")
                .then().statusCode(400)
                .and().body("violations[0].field", equalTo("add.arg0.title"))
                .and().body("violations[0].message", equalTo("must not be blank"));
    }

    @Test
    void shouldNotUpdateAuthor() {
        JsonObject author = Json.createObjectBuilder()
                .add("id", DOSTOEVSKY_ID)
                .add("name", "test")
                .add("dob", DOSTOEVSKY_DOB)
                .build();
        given().contentType("application/json")
                .and().body(author)
                .when().put("/authors/" + DOSTOEVSKY_ID)
                .then().statusCode(405);
    }

    @Test
    void shouldCreateUpdateAndDeleteBook() {
        JsonObject author = Json.createObjectBuilder()
                .add("id", DOSTOEVSKY_ID)
                .add("name", DOSTOEVSKY_NAME)
                .add("dob", DOSTOEVSKY_DOB)
                .build();
        JsonObject book = Json.createObjectBuilder()
                .add("title", "The Brothers Karamazov")
                .add("author", author)
                .build();
        Response response = given().accept("application/json")
                .and().contentType("application/json")
                .and().body(book.toString())
                .when().put("/books/100")
                .thenReturn();
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.header("Location")).isNotEmpty();
        assertThat(response.body().jsonPath().getString("title")).isEqualTo("The Brothers Karamazov");

        String location = response.header("Location");
        JsonObject updateBook = Json.createObjectBuilder()
                .add("title", "Notes from Underground")
                .add("author", author)
                .build();
        given().accept("application/json")
                .and().contentType("application/json")
                .and().body(updateBook.toString())
                .when().put(location)
                .then().statusCode(204);
        given().accept("application/json")
                .when().get(location)
                .then().body("title", is(equalTo("Notes from Underground")));
        when().delete(location)
                .then().statusCode(204);
    }

    @Test
    void shouldNotUpdateBookWithBlankTitle() {
        JsonObject author = Json.createObjectBuilder()
                .add("id", DOSTOEVSKY_ID)
                .add("name", DOSTOEVSKY_NAME)
                .add("dob", DOSTOEVSKY_DOB)
                .build();
        JsonObject book = Json.createObjectBuilder()
                .add("title", "")
                .add("author", author)
                .build();
        given().accept("application/json")
                .and().contentType("application/json")
                .and().body(book.toString())
                .when().put("/books/" + CRIME_AND_PUNISHMENT_ID)
                .then().statusCode(400)
                .and().body("violations[0].field", equalTo("update.arg1.title"))
                .and().body("violations[0].message", equalTo("must not be blank"));
    }
}
