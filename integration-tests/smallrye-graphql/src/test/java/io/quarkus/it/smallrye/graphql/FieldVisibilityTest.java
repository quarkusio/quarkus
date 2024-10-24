package io.quarkus.it.smallrye.graphql;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class FieldVisibilityTest {
    @Test
    void testSchemaWithInvisibleFields() {
        given()
                .when()
                .accept(MediaType.APPLICATION_JSON)
                .get("/graphql/schema.graphql")
                .then()
                .statusCode(200)
                .and().body(containsString("type Book {\n  author: String\n}"))
                .and().body(containsString("input BookInput {\n  author: String\n}"))
                .and().body(containsString("type Customer {\n  address: String\n}"))
                .and().body(containsString("type Purchase {\n  book: Book\n  customer: Customer\n}"));

    }
}
