package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Reproducer for https://github.com/quarkusio/quarkus/issues/57921
 *
 * When Jackson is asked to update an existing instance (e.g. with {@code ObjectMapper#readerForUpdating}, as typically
 * done to apply a JSON merge patch) it invokes {@code ValueDeserializer#deserialize(JsonParser, DeserializationContext, T)}.
 * The generated reflection-free deserializers used to inherit its default implementation, which ignores the provided
 * instance and creates a brand new one, so every property not present in the patch was lost.
 */
public class ReaderForUpdatingReflectionFreeTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Book.class, ArrayBook.class, Attributes.class, Shop.class,
                    Address.class, Movie.class, Library.class, UpdateResource.class))
            .overrideConfigKey("quarkus.rest.jackson.optimization.enable-reflection-free-serializers", "true");

    @Test
    void generatedDeserializersAreUsed() {
        for (String type : List.of("book", "array-book", "attributes", "shop", "movie")) {
            RestAssured.get("/update/deserializer/" + type)
                    .then()
                    .statusCode(200)
                    .body(endsWith("$quarkusjacksondeserializer"));
        }
        // Library is not a body parameter of any endpoint, so it is deserialized by Jackson
        RestAssured.get("/update/deserializer/library")
                .then()
                .statusCode(200)
                .body(not(endsWith("$quarkusjacksondeserializer")));
    }

    @Test
    void streamingDeserializerUpdatesOnlyGivenProperties() {
        patch("book", "{\"pages\": 480}")
                .body("pages", is(480))
                .body("title", is("Booktitle"))
                .body("author", is("Bookauthor"))
                .body("publishedOn", notNullValue());
    }

    @Test
    void treeDeserializerUpdatesOnlyGivenProperties() {
        // the @JsonUnwrapped field makes the generated deserializer use the tree-based approach
        patch("shop", "{\"address_city\": \"Rome\"}")
                .body("name", is("Bookshop"))
                .body("address_city", is("Rome"));
    }

    @Test
    void arrayShapedDeserializerUpdatesOnlyGivenProperties() {
        patch("array-book", "[\"New title\", 480]")
                .body("$", contains("New title", 480));
        // like Jackson, the properties beyond the end of a too short array keep their value
        patch("array-book", "[\"New title\"]")
                .body("$", contains("New title", 400));
    }

    @Test
    void mapDeserializerUpdatesOnlyGivenEntries() {
        patch("attributes", "{\"pages\": 480}")
                .body("title", is("Booktitle"))
                .body("pages", is(480));
    }

    @Test
    void creatorDeserializerUpdatesOnlyGivenProperties() {
        // the properties bound to the @JsonCreator parameters are written through their public fields
        patch("movie", "{\"year\": 1999}")
                .body("title", is("The Matrix"))
                .body("year", is(1999));
        // while a new instance is still created through the constructor
        RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"title\": \"Dune\", \"year\": 2021}")
                .when().post("/update/movie")
                .then()
                .statusCode(200)
                .body("title", is("Dune"))
                .body("year", is(2021));
    }

    @Test
    void nestedMergedPropertyIsUpdatedInPlace() {
        // Library is deserialized by Jackson, which delegates the update of its @JsonMerge field to the generated
        // deserializer of Book
        patch("library", "{\"book\": {\"pages\": 480}}")
                .header("same-nested-instance", is("true"))
                .body("name", is("Central library"))
                .body("book.pages", is(480))
                .body("book.title", is("Booktitle"));
    }

    private static ValidatableResponse patch(String type, String mergePatch) {
        return RestAssured.given()
                .contentType(MediaType.APPLICATION_MERGE_PATCH_JSON)
                .body(mergePatch)
                .when().patch("/update/" + type)
                .then()
                .statusCode(200)
                .header("same-instance", is("true"));
    }

    public static class Book {
        public String title;
        public String author;
        public int pages;
        public LocalDate publishedOn;
    }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({ "title", "pages" })
    public static class ArrayBook {
        public String title;
        public int pages;
    }

    public static class Attributes extends HashMap<String, Object> {
    }

    public static class Shop {
        public String name;
        @JsonUnwrapped(prefix = "address_")
        public Address address;
    }

    public static class Address {
        public String street;
        public String city;
    }

    public static class Movie {
        public String title;
        public int year;

        public Movie() {
        }

        @JsonCreator
        public Movie(@JsonProperty("title") String title, @JsonProperty("year") int year) {
            this.title = title;
            this.year = year;
        }
    }

    public static class Library {
        public String name;
        @JsonMerge
        public Book book;
    }

    @Path("/update")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class UpdateResource {

        @Inject
        ObjectMapper objectMapper;

        // the body parameters of these methods are what triggers the generation of the reflection-free deserializers
        @POST
        @Path("book")
        public Book create(Book book) {
            return book;
        }

        @POST
        @Path("array-book")
        public ArrayBook create(ArrayBook book) {
            return book;
        }

        @POST
        @Path("attributes")
        public Attributes create(Attributes attributes) {
            return attributes;
        }

        @POST
        @Path("shop")
        public Shop create(Shop shop) {
            return shop;
        }

        @POST
        @Path("movie")
        public Movie create(Movie movie) {
            return movie;
        }

        @PATCH
        @Path("{type}")
        @Consumes(MediaType.APPLICATION_MERGE_PATCH_JSON)
        public Response patch(@PathParam("type") String type, String mergePatch) {
            Object existing = existing(type);
            Book nested = existing instanceof Library library ? library.book : null;
            Object updated = objectMapper.readerForUpdating(existing).readValue(mergePatch);
            return Response.ok(updated)
                    .header("same-instance", updated == existing)
                    .header("same-nested-instance", nested != null && ((Library) updated).book == nested)
                    .build();
        }

        @GET
        @Path("deserializer/{type}")
        @Produces(MediaType.TEXT_PLAIN)
        public String deserializer(@PathParam("type") String type) {
            return objectMapper._deserializationContext()
                    .findRootValueDeserializer(objectMapper.constructType(existing(type).getClass()))
                    .getClass().getName();
        }

        private static Object existing(String type) {
            return switch (type) {
                case "book" -> book();
                case "array-book" -> arrayBook();
                case "attributes" -> attributes();
                case "shop" -> shop();
                case "movie" -> movie();
                case "library" -> library();
                default -> throw new NotFoundException();
            };
        }

        private static Book book() {
            Book book = new Book();
            book.title = "Booktitle";
            book.author = "Bookauthor";
            book.pages = 400;
            book.publishedOn = LocalDate.of(1900, 1, 1);
            return book;
        }

        private static ArrayBook arrayBook() {
            ArrayBook book = new ArrayBook();
            book.title = "Booktitle";
            book.pages = 400;
            return book;
        }

        private static Attributes attributes() {
            Attributes attributes = new Attributes();
            attributes.put("title", "Booktitle");
            attributes.put("pages", 400);
            return attributes;
        }

        private static Shop shop() {
            Shop shop = new Shop();
            shop.name = "Bookshop";
            shop.address = new Address();
            shop.address.street = "Main street";
            shop.address.city = "Milan";
            return shop;
        }

        private static Movie movie() {
            return new Movie("The Matrix", 1998);
        }

        private static Library library() {
            Library library = new Library();
            library.name = "Central library";
            library.book = book();
            return library;
        }
    }
}
