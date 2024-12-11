package io.quarkus.smallrye.graphql.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.jboss.resteasy.reactive.RestResponse.StatusCode.OK;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ExcludeNullFieldsInResponseTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestApi.class, Book.class, Author.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                    .addAsResource(
                            new StringAsset(
                                    "quarkus.smallrye-graphql.exclude-null-fields-in-responses=true"),
                            "application.properties"));

    @Test
    void testExcludeNullFieldsInResponse() {
        final String request = getPayload("""
                {
                  books {
                    name
                    pages
                    author {
                      firstName
                      lastName
                    }
                  }
                }""");
        given()
                .when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(request)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(OK)
                .and()
                .body(containsString("{\"data\":{" +
                        "\"books\":[{" +
                        "\"name\":\"The Hobbit\"," +
                        // missing null field
                        "\"author\":{" +
                        "\"firstName\":\"J.R.R.\"" +
                        // missing null field
                        "}" +
                        "},{" +
                        "\"name\":\"The Lord of the Rings\"," +
                        "\"pages\":1178," +
                        "\"author\":{" +
                        "\"firstName\":\"J.R.R.\"," +
                        "\"lastName\":\"Tolkien\"" +
                        "}" +
                        "}]" +
                        "}}"));
    }

    @GraphQLApi
    public static class TestApi {
        @Query
        public Book[] getBooks() {
            return new Book[] {
                    new Book("The Hobbit", null, new Author("J.R.R.", null)),
                    new Book("The Lord of the Rings", 1178, new Author("J.R.R.", "Tolkien"))
            };
        }
    }

    public record Book(String name, Integer pages, Author author) {
    }

    public record Author(String firstName, String lastName) {
    }
}
