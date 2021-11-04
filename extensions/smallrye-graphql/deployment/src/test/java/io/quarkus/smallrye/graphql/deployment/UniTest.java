package io.quarkus.smallrye.graphql.deployment;

import static io.quarkus.smallrye.graphql.deployment.AbstractGraphQLTest.MEDIATYPE_JSON;
import static io.quarkus.smallrye.graphql.deployment.AbstractGraphQLTest.getPropertyAsString;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;
import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.smallrye.mutiny.Uni;

/**
 * Make sure that Uni works
 */
public class UniTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BookGraphQLApi.class, Book.class)
                    .addAsResource(new StringAsset(getPropertyAsString()), "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testSourcePost() {
        String booksRequest = getPayload("{\n" +
                "    book(name:\"Lord of the Flies\"){\n" +
                "       title\n" +
                "       published\n" +
                "       buyLink\n" +
                "       authors {\n" +
                "           name\n" +
                "           bornName\n" +
                "       }\n" +
                "    }\n" +
                "}");

        RestAssured.given()
                .filter(new ResponseLoggingFilter())
                .when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(booksRequest)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(CoreMatchers.containsString("Lord of the Flies"),
                        CoreMatchers.containsString("https://www.amazon.com/s?k=Lord+of+the+Flies&i=stripbooks-intl-ship"), // Test source
                        CoreMatchers.containsString("William Gerald Golding")); // Test batch

    }

    @GraphQLApi
    public static class BookGraphQLApi {

        @Query
        public Uni<Book> getBook(String name) {
            return Uni.createFrom().item(() -> BOOKS.get(name));
        }

        public Uni<String> getBuyLink(@Source Book book) {
            String title = book.title.replaceAll(" ", "+");
            return Uni.createFrom().item(() -> String.format(AMAZON_SEARCH_FORMAT, title));
        }

        public Uni<List<List<Author>>> getAuthors(@Source List<Book> books) {
            List<List<Author>> authorsOfAllBooks = new ArrayList<>();
            for (Book book : books) {
                List<Author> authors = new ArrayList<>();
                for (String name : book.authors) {
                    authors.add(AUTHORS.get(name));
                }
                authorsOfAllBooks.add(authors);
            }
            return Uni.createFrom().item(() -> authorsOfAllBooks);
        }

        private static final String AMAZON_SEARCH_FORMAT = "https://www.amazon.com/s?k=%s&i=stripbooks-intl-ship";
        private static Map<String, Book> BOOKS = new HashMap<>();
        private static Map<String, Author> AUTHORS = new HashMap<>();
        static {
            Book book1 = new Book("0-571-05686-5", "Lord of the Flies", LocalDate.of(1954, Month.SEPTEMBER, 17),
                    "William Golding");
            BOOKS.put(book1.title, book1);
            AUTHORS.put("William Golding", new Author("William Golding", "William Gerald Golding",
                    LocalDate.of(1911, Month.SEPTEMBER, 19), "Newquay, Cornwall, England"));

            Book book2 = new Book("0-582-53008-3", "Animal Farm", LocalDate.of(1945, Month.AUGUST, 17), "George Orwell");
            BOOKS.put(book2.title, book2);
            AUTHORS.put("George Orwell", new Author("George Orwell", "Eric Arthur Blair", LocalDate.of(1903, Month.JUNE, 25),
                    "Motihari, Bengal Presidency, British India"));
        }
    }

    public static class Book {
        public String isbn;
        public String title;
        public LocalDate published;
        public List<String> authors;

        public Book() {
        }

        public Book(String isbn, String title, LocalDate published, String... authors) {
            this.isbn = isbn;
            this.title = title;
            this.published = published;
            this.authors = Arrays.asList(authors);
        }

        @Override
        public String toString() {
            return "Book{" + "isbn=" + isbn + ", title=" + title + ", published=" + published + ", authors=" + authors + '}';
        }
    }

    public static class Author {
        public String name;
        public String bornName;
        public LocalDate birthDate;
        public String birthPlace;

        public Author() {
        }

        public Author(String name, String bornName, LocalDate birthDate, String birthPlace) {
            this.name = name;
            this.bornName = bornName;
            this.birthDate = birthDate;
            this.birthPlace = birthPlace;
        }
    }
}
