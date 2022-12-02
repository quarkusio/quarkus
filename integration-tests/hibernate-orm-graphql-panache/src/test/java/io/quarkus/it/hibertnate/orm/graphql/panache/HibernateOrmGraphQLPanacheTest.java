package io.quarkus.it.hibertnate.orm.graphql.panache;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class HibernateOrmGraphQLPanacheTest {

    private static final int DOSTOEVSKY_ID = 1;

    private static final String DOSTOEVSKY_NAME = "Fyodor Dostoevsky";

    private static final String DOSTOEVSKY_DOB = "1821-11-11";

    private static final int CRIME_AND_PUNISHMENT_ID = 2;

    private static final String CRIME_AND_PUNISHMENT_TITLE = "Crime and Punishment";

    private static final int IDIOT_ID = 3;

    private static final String IDIOT_TITLE = "Idiot";

    private static final int DEMONS_ID = 4;

    private static final String DEMONS_TITLE = "Demons";

    private static final int THE_ADOLESCENT_ID = 5;

    private static final String THE_ADOLESCENT_TITLE = "The adolescent";

    @Test
    void testEndpoint() {

        String bookAndAuthorRequest = PayloadCreator.getPayload("{\n" +
                "  authors {\n" +
                "    id\n" +
                "    name\n" +
                "    dob\n" +
                "  }\n" +
                "  books {\n" +
                "    id\n" +
                "    title\n" +
                "    author {\n" +
                "      name\n" +
                "    }\n" +
                "  }\n" +
                "}");

        given()
                .when()
                .accept("application/json")
                .contentType("application/json")
                .body(bookAndAuthorRequest)
                .post("/graphql")
                .then()
                .statusCode(200)
                .and().body("data.authors.id", contains(DOSTOEVSKY_ID))
                .and().body("data.authors.name", contains(DOSTOEVSKY_NAME))
                .and().body("data.authors.dob", contains(DOSTOEVSKY_DOB))
                .and().body("data.books.id", contains(CRIME_AND_PUNISHMENT_ID, IDIOT_ID, DEMONS_ID, THE_ADOLESCENT_ID))
                .and().body("data.books.title",
                        contains(CRIME_AND_PUNISHMENT_TITLE, IDIOT_TITLE, DEMONS_TITLE, THE_ADOLESCENT_TITLE));
    }

}
