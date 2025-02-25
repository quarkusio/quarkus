package io.quarkus.smallrye.graphql.deployment.fieldvisibility;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FieldVisibilityTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FieldVisibilityResource.Book.class, FieldVisibilityResource.Customer.class,
                            FieldVisibilityResource.Purchase.class, FieldVisibilityResource.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                    .addAsResource(
                            new StringAsset(
                                    "quarkus.smallrye-graphql.field-visibility=Purchase.count,Customer.name,Book.*\\.title"),
                            "application.properties"));

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
