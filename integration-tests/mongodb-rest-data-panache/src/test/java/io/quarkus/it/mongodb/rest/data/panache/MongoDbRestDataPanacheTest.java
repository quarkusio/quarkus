package io.quarkus.it.mongodb.rest.data.panache;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

@QuarkusTest
@DisabledOnOs(OS.WINDOWS)
class MongoDbRestDataPanacheTest {

    private Author dostoevsky;

    private Book crimeAndPunishment;

    private Book idiot;

    @BeforeEach
    void beforeEach() {
        dostoevsky = createTestAuthor("Fyodor Dostoevsky", "1821-11-11");
        crimeAndPunishment = createTestBook("Crime and Punishment", dostoevsky);
        idiot = createTestBook("Idiot", dostoevsky);
    }

    @AfterEach
    void afterEach() {
        deleteTestBooks();
        deleteTestAuthors();
    }

    @Test
    void shouldGetAuthor() {
        given().accept("application/json")
                .when().get("/authors/" + dostoevsky.id)
                .then().statusCode(200)
                .and().body("id", is(equalTo(dostoevsky.id.toString())))
                .and().body("name", is(equalTo(dostoevsky.name)))
                .and().body("dob", is(equalTo(dostoevsky.dob.toString())));
    }

    @Test
    void shouldNotGetAuthorHal() {
        given().accept("application/hal+json")
                .when().get("/authors/" + dostoevsky.id)
                .then().statusCode(406);
    }

    @Test
    void shouldGetBook() {
        given().accept("application/json")
                .when().get("/books/" + crimeAndPunishment.getId())
                .then().statusCode(200)
                .and().body("id", is(equalTo(crimeAndPunishment.getId().toString())))
                .and().body("title", is(equalTo(crimeAndPunishment.getTitle())))
                .and().body("author.id", is(equalTo(dostoevsky.id.toString())))
                .and().body("author.name", is(equalTo(dostoevsky.name)))
                .and().body("author.dob", is(equalTo(dostoevsky.dob.toString())));
    }

    @Test
    void shouldGetBookHal() {
        given().accept("application/hal+json")
                .when().get("/books/" + crimeAndPunishment.getId())
                .then().statusCode(200)
                .and().body("id", is(equalTo(crimeAndPunishment.getId().toString())))
                .and().body("title", is(equalTo(crimeAndPunishment.getTitle())))
                .and().body("author.id", is(equalTo(dostoevsky.id.toString())))
                .and().body("author.name", is(equalTo(dostoevsky.name)))
                .and().body("author.dob", is(equalTo(dostoevsky.dob.toString())))
                .and().body("_links.add.href", endsWith("/books"))
                .and().body("_links.list.href", endsWith("/books"))
                .and().body("_links.self.href", endsWith("/books/" + crimeAndPunishment.getId()))
                .and().body("_links.update.href", endsWith("/books/" + crimeAndPunishment.getId()))
                .and().body("_links.remove.href", endsWith("/books/" + crimeAndPunishment.getId()));
    }

    @Test
    void shouldListAuthors() {
        given().accept("application/json")
                .when().get("/authors")
                .then().statusCode(200)
                .and().body("id", contains(dostoevsky.id.toString()))
                .and().body("name", contains(dostoevsky.name))
                .and().body("dob", contains(dostoevsky.dob.toString()));
    }

    @Test
    void shouldListBooks() {
        given().accept("application/json")
                .when().get("/books")
                .then().statusCode(200)
                .and().body("id", contains(crimeAndPunishment.getId().toString(), idiot.getId().toString()))
                .and().body("title", contains(crimeAndPunishment.getTitle(), idiot.getTitle()))
                .and().body("author.id", contains(dostoevsky.id.toString(), dostoevsky.id.toString()))
                .and().body("author.name", contains(dostoevsky.name, dostoevsky.name))
                .and().body("author.dob", contains(dostoevsky.dob.toString(), dostoevsky.dob.toString()));
    }

    @Test
    void shouldListBooksHal() {
        given().accept("application/hal+json")
                .when().get("/books")
                .then().statusCode(200)
                .and().body("_embedded.books.id", contains(crimeAndPunishment.getId().toString(), idiot.getId().toString()))
                .and().body("_embedded.books.title", contains(crimeAndPunishment.getTitle(), idiot.getTitle()))
                .and().body("_embedded.books.author.id", contains(dostoevsky.id.toString(), dostoevsky.id.toString()))
                .and().body("_embedded.books.author.name", contains(dostoevsky.name, dostoevsky.name))
                .and().body("_embedded.books.author.dob", contains(dostoevsky.dob.toString(), dostoevsky.dob.toString()))
                .and().body("_embedded.books._links.add.href", contains(endsWith("/books"), endsWith("/books")))
                .and().body("_embedded.books._links.list.href", contains(endsWith("/books"), endsWith("/books")))
                .and().body("_embedded.books._links.self.href",
                        contains(endsWith("/books/" + crimeAndPunishment.getId()), endsWith("/books/" + idiot.getId())))
                .and().body("_embedded.books._links.update.href",
                        contains(endsWith("/books/" + crimeAndPunishment.getId()), endsWith("/books/" + idiot.getId())))
                .and().body("_embedded.books._links.remove.href",
                        contains(endsWith("/books/" + crimeAndPunishment.getId()), endsWith("/books/" + idiot.getId())))
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
        when().delete("/authors/" + dostoevsky.id)
                .then().statusCode(405);
    }

    @Test
    void shouldCreateAndDeleteBook() {
        String title = "The Brothers Karamazov";
        JsonObject book = Json.createObjectBuilder()
                .add("id", titleToId(title))
                .add("title", title)
                .add("author", Json.createObjectBuilder()
                        .add("id", dostoevsky.id.toString())
                        .add("name", dostoevsky.name)
                        .add("dob", dostoevsky.dob.toString())
                        .build())
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
    void shouldNotCreateDuplicateBook() {
        JsonObject book = Json.createObjectBuilder()
                .add("id", titleToId(idiot.getId()))
                .add("title", idiot.getTitle())
                .add("author", Json.createObjectBuilder()
                        .add("id", dostoevsky.id.toString())
                        .add("name", dostoevsky.name)
                        .add("dob", dostoevsky.dob.toString())
                        .build())
                .build();
        given().accept("application/json")
                .and().contentType("application/json")
                .and().body(book.toString())
                .when().post("/books")
                .then().statusCode(409);
    }

    @Test
    void shouldNotUpdateAuthor() {
        JsonObject author = Json.createObjectBuilder()
                .add("id", dostoevsky.id.toString())
                .add("name", "test")
                .add("dob", dostoevsky.dob.toString())
                .build();
        given().contentType("application/json")
                .and().body(author.toString())
                .when().put("/authors/" + dostoevsky.id)
                .then().statusCode(405);
    }

    @Test
    void shouldCreateUpdateAndDeleteBook() {
        JsonObject dostoevskyJson = Json.createObjectBuilder()
                .add("id", dostoevsky.id.toString())
                .add("name", dostoevsky.name)
                .add("dob", dostoevsky.dob.toString())
                .build();
        String title = "The Brothers Karamazov";
        String id = titleToId(title);
        JsonObject book = Json.createObjectBuilder()
                .add("id", id)
                .add("title", title)
                .add("author", dostoevskyJson)
                .build();
        Response response = given().accept("application/json")
                .and().contentType("application/json")
                .and().body(book.toString())
                .when().put("/books/" + id)
                .thenReturn();
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.header("Location")).isNotEmpty();
        assertThat(response.body().jsonPath().getString("id")).isEqualTo(id);
        assertThat(response.body().jsonPath().getString("title")).isEqualTo(title);

        String location = response.header("Location");
        JsonObject updateBook = Json.createObjectBuilder()
                .add("title", "Notes from Underground")
                .add("author", dostoevskyJson)
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

    private Author createTestAuthor(String name, String dob) {
        return given().contentType(MediaType.APPLICATION_JSON)
                .and().accept(MediaType.APPLICATION_JSON)
                .and().body(Json.createObjectBuilder()
                        .add("name", name)
                        .add("dob", dob)
                        .build()
                        .toString())
                .post("/test/authors")
                .thenReturn()
                .as(Author.class);
    }

    private void deleteTestAuthors() {
        when().delete("/test/authors")
                .then().statusCode(204);
    }

    private Book createTestBook(String title, Author author) {
        return given().contentType(MediaType.APPLICATION_JSON)
                .and().accept(MediaType.APPLICATION_JSON)
                .and().body(Json.createObjectBuilder()
                        .add("id", titleToId(title))
                        .add("title", title)
                        .add("author", Json.createObjectBuilder()
                                .add("id", author.id.toString())
                                .add("name", author.name)
                                .add("dob", author.dob.toString())
                                .build())
                        .build()
                        .toString())
                .post("/test/books")
                .thenReturn()
                .as(Book.class);
    }

    private String titleToId(String title) {
        return title.toLowerCase().replaceAll(" ", "-");
    }

    private void deleteTestBooks() {
        when().delete("/test/books")
                .then().statusCode(204);
    }
}
