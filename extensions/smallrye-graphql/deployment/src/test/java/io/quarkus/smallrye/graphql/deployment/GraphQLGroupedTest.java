package io.quarkus.smallrye.graphql.deployment;

import static io.quarkus.smallrye.graphql.deployment.AbstractGraphQLTest.MEDIATYPE_JSON;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;
import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Basic tests for Grouping.
 */
public class GraphQLGroupedTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BookGraphQLApi.class, Book.class)
                    .addAsResource(new StringAsset(getPropertyAsString()), "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testSourcePost() {
        String booksRequest = getPayload("{\n" +
                "  books{\n" +
                "    book(name:\"Lord of the Flies\"){\n" +
                "      title\n" +
                "      authors\n" +
                "      published\n" +
                "    }\n" +
                "  }\n" +
                "}");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(booksRequest)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(CoreMatchers.containsString("Lord of the Flies"));

    }

    @GraphQLApi
    @Name("books")
    @Description("Allow all book releated APIs")
    public static class BookGraphQLApi {

        @Query
        public List<Book> getAllBooks() {
            return new ArrayList<>(BOOKS.values());
        }

        @Query
        public Book getBook(String name) {
            return BOOKS.get(name);
        }

        @Mutation
        public Book addBook(Book book) {
            BOOKS.put(book.title, book);
            return book;
        }

        private static Map<String, Book> BOOKS = new HashMap<>();
        static {
            Book book1 = new Book("0-571-05686-5", "Lord of the Flies", LocalDate.of(1954, Month.SEPTEMBER, 17),
                    "William Golding");
            BOOKS.put(book1.title, book1);

            Book book2 = new Book("0-582-53008-3", "Animal Farm", LocalDate.of(1945, Month.AUGUST, 17), "George Orwell");
            BOOKS.put(book2.title, book2);
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
    }

}
