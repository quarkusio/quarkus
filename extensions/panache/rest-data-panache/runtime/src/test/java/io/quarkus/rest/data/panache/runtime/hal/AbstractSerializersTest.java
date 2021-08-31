package io.quarkus.rest.data.panache.runtime.hal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.util.Collections;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.jupiter.api.Test;

abstract class AbstractSerializersTest {

    abstract String toJson(Object object);

    @Test
    void shouldSerializeOneBook() {
        int id = 1;
        String title = "Black Swan";
        Book book = usePublishedBook() ? new PublishedBook(id, title) : new Book(id, title);
        JsonReader jsonReader = Json.createReader(new StringReader(toJson(new HalEntityWrapper(book))));

        assertBook(book, jsonReader.readObject());
    }

    @Test
    void shouldSerializeOneBookWithNullName() {
        int id = 1;
        String title = null;
        Book book = usePublishedBook() ? new PublishedBook(id, title) : new Book(id, title);
        JsonReader jsonReader = Json.createReader(new StringReader(toJson(new HalEntityWrapper(book))));

        assertBook(book, jsonReader.readObject());
    }

    @Test
    void shouldSerializeCollectionOfBooks() {
        int id = 1;
        String title = "Black Swan";
        Book book = usePublishedBook() ? new PublishedBook(id, title) : new Book(id, title);
        HalCollectionWrapper wrapper = new HalCollectionWrapper(Collections.singleton(book), Book.class, "books");
        JsonReader jsonReader = Json.createReader(new StringReader(toJson(wrapper)));
        JsonObject collectionJson = jsonReader.readObject();

        assertBook(book, collectionJson.getJsonObject("_embedded").getJsonArray("books").getJsonObject(0));

        JsonObject collectionLinksJson = collectionJson.getJsonObject("_links");
        assertThat(collectionLinksJson.getJsonObject("list").getString("href")).isEqualTo("/books");
        assertThat(collectionLinksJson.getJsonObject("add").getString("href")).isEqualTo("/books");
    }

    private void assertBook(Book book, JsonObject bookJson) {
        assertThat(bookJson.getInt("id")).isEqualTo(book.id);
        if (bookJson.isNull("book-name")) {
            assertThat(book.getName()).isNull();
        } else {
            assertThat(bookJson.getString("book-name")).isEqualTo(book.getName());
        }
        assertThat(bookJson.containsKey("ignored")).isFalse();

        JsonObject bookLinksJson = bookJson.getJsonObject("_links");
        assertThat(bookLinksJson.getJsonObject("self").getString("href")).isEqualTo("/books/" + book.id);
        assertThat(bookLinksJson.getJsonObject("update").getString("href")).isEqualTo("/books/" + book.id);
        assertThat(bookLinksJson.getJsonObject("list").getString("href")).isEqualTo("/books");
        assertThat(bookLinksJson.getJsonObject("add").getString("href")).isEqualTo("/books");
    }

    protected abstract boolean usePublishedBook();
}
