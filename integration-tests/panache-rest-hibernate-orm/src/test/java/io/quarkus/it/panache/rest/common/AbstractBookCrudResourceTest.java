package io.quarkus.it.panache.rest.common;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import io.restassured.response.Response;

public abstract class AbstractBookCrudResourceTest {

    private static final String APPLICATION_HAL_JSON = "application/hal+json";

    private static final AuthorDto DOSTOEVSKY = new AuthorDto(1L, "Fyodor Dostoevsky", LocalDate.of(1821, 11, 11));

    private static final AuthorDto ORWELL = new AuthorDto(2L, "George Orwell", LocalDate.of(1903, 06, 25));

    private static final List<ReviewDto> CRIME_AND_PUNISHMENT_REVIEWS = Arrays.asList(
            new ReviewDto("first", "Captivating"),
            new ReviewDto("second", "Amazing"));

    protected abstract String getResourceName();

    @BeforeEach
    void setUp() {
        RestAssured.basePath = getResourceName();
    }

    @AfterEach
    void tearDown() {
        RestAssured.basePath = "";
    }

    @Test
    void shouldGetOne() {
        Response response = get("3", APPLICATION_JSON);
        assertThat(response.getStatusCode()).isEqualTo(200);

        BookDto expectedBook = new BookDto(3L, "Crime and Punishment", DOSTOEVSKY, CRIME_AND_PUNISHMENT_REVIEWS);
        JsonObject actualBook = Json.createReader(response.getBody().asInputStream()).readObject();

        assertBook(expectedBook, actualBook);
    }

    @Test
    void shouldGetOneWithHal() {
        Response response = get("3", APPLICATION_HAL_JSON);
        assertThat(response.getStatusCode()).isEqualTo(200);

        BookDto expectedBook = new BookDto(3L, "Crime and Punishment", DOSTOEVSKY, CRIME_AND_PUNISHMENT_REVIEWS);
        JsonObject actualBook = Json.createReader(response.getBody().asInputStream()).readObject();

        assertHalBook(expectedBook, actualBook);
    }

    @Test
    void shouldGetAll() {
        Response response = getAll(APPLICATION_JSON);
        assertThat(response.getStatusCode()).isEqualTo(200);

        BookDto firstExpectedBook = new BookDto(3L, "Crime and Punishment", DOSTOEVSKY, CRIME_AND_PUNISHMENT_REVIEWS);
        BookDto secondExpectedBook = new BookDto(4L, "Idiot", DOSTOEVSKY);
        JsonArray actualBooks = Json.createReader(response.getBody().asInputStream()).readArray();

        assertBook(firstExpectedBook, actualBooks.getJsonObject(0));
        assertBook(secondExpectedBook, actualBooks.getJsonObject(1));
    }

    @Test
    void shouldGetAllBooksWithHal() {
        Response response = getAll(APPLICATION_HAL_JSON);
        assertThat(response.getStatusCode()).isEqualTo(200);

        JsonObject responseJson = Json.createReader(response.getBody().asInputStream()).readObject();

        Map<String, String> expectedCollectionLinkEndings = new HashMap<>();
        expectedCollectionLinkEndings.put("list", "/" + getResourceName());
        expectedCollectionLinkEndings.put("add", "/" + getResourceName());
        assertLinksEndWith(expectedCollectionLinkEndings, responseJson.getJsonObject("_links"));

        BookDto firstExpectedBook = new BookDto(3L, "Crime and Punishment", DOSTOEVSKY, CRIME_AND_PUNISHMENT_REVIEWS);
        BookDto secondExpectedBook = new BookDto(4L, "Idiot", DOSTOEVSKY);
        JsonArray actualBooks = responseJson.getJsonObject("_embedded").getJsonArray(getResourceName());

        assertHalBook(firstExpectedBook, actualBooks.getJsonObject(0));
        assertHalBook(secondExpectedBook, actualBooks.getJsonObject(1));
    }

    @Test
    void shouldNotGetNonExistentBook() {
        get("100", APPLICATION_JSON)
                .then().statusCode(404);
    }

    @Test
    void shouldCreateAndDelete() {
        Response postResponse = create("Animal Farm", ORWELL, APPLICATION_JSON);
        assertThat(postResponse.getStatusCode()).isEqualTo(201);

        String newBookUrl = postResponse.getHeader("Location");
        Long newBookId = Long.valueOf(newBookUrl.substring(newBookUrl.lastIndexOf("/") + 1));
        BookDto expectedBookFromPost = new BookDto(newBookId, "Animal Farm", ORWELL);
        JsonObject actualBookFromPost = Json.createReader(postResponse.getBody().asInputStream()).readObject();
        assertBook(expectedBookFromPost, actualBookFromPost);

        Response getResponse = get(newBookUrl, APPLICATION_JSON);
        assertThat(getResponse.getStatusCode()).isEqualTo(200);

        JsonObject actualBookFromGet = Json.createReader(getResponse.getBody().asInputStream()).readObject();
        BookDto expectedBookFromGet = new BookDto(newBookId, "Animal Farm", ORWELL);
        assertBook(expectedBookFromGet, actualBookFromGet);

        delete(newBookUrl)
                .then().statusCode(204);
        get(newBookUrl, APPLICATION_JSON)
                .then().statusCode(404);
    }

    @Test
    void shouldCreateWithHalAndDelete() {
        Response postResponse = create("Animal Farm", ORWELL, APPLICATION_HAL_JSON);
        assertThat(postResponse.getStatusCode()).isEqualTo(201);

        String newBookUrl = postResponse.getHeader("Location");
        Long newBookId = Long.valueOf(newBookUrl.substring(newBookUrl.lastIndexOf("/") + 1));
        BookDto expectedBookFromPost = new BookDto(newBookId, "Animal Farm", ORWELL);
        JsonObject actualBookFromPost = Json.createReader(postResponse.getBody().asInputStream()).readObject();
        assertHalBook(expectedBookFromPost, actualBookFromPost);

        Response getResponse = get(newBookUrl, APPLICATION_HAL_JSON);
        assertThat(getResponse.getStatusCode()).isEqualTo(200);

        BookDto expectedBookFromGet = new BookDto(newBookId, "Animal Farm", ORWELL);
        JsonObject actualBookFromGet = Json.createReader(getResponse.getBody().asInputStream()).readObject();
        assertHalBook(expectedBookFromGet, actualBookFromGet);

        delete(newBookUrl)
                .then().statusCode(204);
        get(newBookUrl, APPLICATION_JSON)
                .then().statusCode(404);
    }

    @Test
    void shouldCreateWithPutAndDelete() {
        Response putResponse = update("100", "Animal Farm", ORWELL, APPLICATION_JSON);
        assertThat(putResponse.getStatusCode()).isEqualTo(201);

        String newBookUrl = putResponse.getHeader("Location");
        Long newBookId = Long.valueOf(newBookUrl.substring(newBookUrl.lastIndexOf("/") + 1));
        BookDto expectedBookFromPut = new BookDto(newBookId, "Animal Farm", ORWELL);
        JsonObject actualBookFromPut = Json.createReader(putResponse.getBody().asInputStream()).readObject();
        assertBook(expectedBookFromPut, actualBookFromPut);

        Response getResponse = get(newBookUrl, APPLICATION_JSON);
        assertThat(getResponse.getStatusCode()).isEqualTo(200);

        BookDto expectedBookFromGet = new BookDto(newBookId, "Animal Farm", ORWELL);
        JsonObject actualBookFromGet = Json.createReader(getResponse.getBody().asInputStream()).readObject();
        assertBook(expectedBookFromGet, actualBookFromGet);

        delete(newBookUrl)
                .then().statusCode(204);
        get(newBookUrl, APPLICATION_JSON)
                .then().statusCode(404);
    }

    @Test
    void shouldCreateWithPutAndHalAndDelete() {
        Response putResponse = update("100", "Animal Farm", ORWELL, APPLICATION_HAL_JSON);
        assertThat(putResponse.getStatusCode()).isEqualTo(201);

        String newBookUrl = putResponse.getHeader("Location");
        Long newBookId = Long.valueOf(newBookUrl.substring(newBookUrl.lastIndexOf("/") + 1));
        BookDto expectedBookFromPut = new BookDto(newBookId, "Animal Farm", ORWELL);
        JsonObject actualBookFromPut = Json.createReader(putResponse.getBody().asInputStream()).readObject();
        assertHalBook(expectedBookFromPut, actualBookFromPut);

        Response getResponse = get(newBookUrl, APPLICATION_HAL_JSON);
        assertThat(getResponse.getStatusCode()).isEqualTo(200);

        BookDto expectedBookFromGet = new BookDto(newBookId, "Animal Farm", ORWELL);
        JsonObject actualBookFromGet = Json.createReader(getResponse.getBody().asInputStream()).readObject();
        assertHalBook(expectedBookFromGet, actualBookFromGet);

        delete(newBookUrl)
                .then().statusCode(204);
        get(newBookUrl, APPLICATION_JSON)
                .then().statusCode(404);
    }

    @Test
    void shouldUpdate() {
        String newName = "Random book name " + UUID.randomUUID();
        update("5", newName, DOSTOEVSKY, APPLICATION_JSON)
                .then().statusCode(204);

        Response getResponse = get("5", APPLICATION_JSON);
        assertThat(getResponse.getStatusCode()).isEqualTo(200);

        BookDto expectedBook = new BookDto(5L, newName, DOSTOEVSKY);
        JsonObject actualBook = Json.createReader(getResponse.getBody().asInputStream()).readObject();
        assertBook(expectedBook, actualBook);
    }

    @Test
    void shouldNotDeleteNonExistent() {
        delete("1000")
                .then().statusCode(404);
    }

    private Response get(String path, String accept) {
        return given().accept(accept)
                .when().get(path)
                .thenReturn();
    }

    private Response getAll(String accept) {
        return given().accept(accept)
                .when().get()
                .thenReturn();
    }

    private Response create(String title, AuthorDto author, String accept) {
        return given().accept(accept)
                .and().contentType(APPLICATION_JSON)
                .and().body(new BookDto(title, author))
                .when().post()
                .thenReturn();
    }

    private Response update(String path, String title, AuthorDto author, String accept) {
        return given().accept(accept)
                .and().contentType(APPLICATION_JSON)
                .and().body(new BookDto(title, author))
                .when().put(path)
                .thenReturn();
    }

    private Response delete(String path) {
        return when().delete(path)
                .thenReturn();
    }

    private void assertBook(BookDto expected, JsonObject actual) {
        assertThat((long) actual.getInt("id")).isEqualTo(expected.id);
        assertThat(actual.getString("title")).isEqualTo(expected.title);
        assertAuthor(expected.author, actual.getJsonObject("author"));
        assertReviews(expected.reviews, actual.getJsonArray("reviews"));
    }

    private void assertAuthor(AuthorDto expected, JsonObject actual) {
        assertThat((long) actual.getInt("id")).isEqualTo(expected.id);
        assertThat(actual.getString("name")).isEqualTo(expected.name);
        assertThat(actual.getString("dob")).isEqualTo(expected.dobAsString());
    }

    private void assertReviews(List<ReviewDto> expectedReviews, JsonArray actualReviews) {
        assertThat(actualReviews.size()).isEqualTo(expectedReviews.size());
        for (int i = 0; i < expectedReviews.size(); i++) {
            assertThat(actualReviews.getJsonObject(i).getString("id")).isEqualTo(expectedReviews.get(i).id);
            assertThat(actualReviews.getJsonObject(i).getString("text")).isEqualTo(expectedReviews.get(i).text);
        }
    }

    private void assertHalBook(BookDto expected, JsonObject actual) {
        assertBook(expected, actual);

        Map<String, String> expectedLinkEndings = new HashMap<>();
        expectedLinkEndings.put("self", "/" + getResourceName() + "/" + expected.id);
        expectedLinkEndings.put("remove", "/" + getResourceName() + "/" + expected.id);
        expectedLinkEndings.put("update", "/" + getResourceName() + "/" + expected.id);
        expectedLinkEndings.put("add", "/" + getResourceName());
        expectedLinkEndings.put("list", "/" + getResourceName());
        assertLinksEndWith(expectedLinkEndings, actual.getJsonObject("_links"));
    }

    private void assertLinksEndWith(Map<String, String> expectedEndings, JsonObject actualLinks) {
        for (AbstractMap.Entry<String, String> expectedEnding : expectedEndings.entrySet()) {
            assertThat(actualLinks.getJsonObject(expectedEnding.getKey()).getString("href"))
                    .endsWith(expectedEnding.getValue());
        }
    }
}