package io.quarkus.smallrye.graphql.deployment.fieldvisibility;

import static io.restassured.RestAssured.given;

import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FieldVisibilityNoIntrospectionTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FieldVisibilityResource.Book.class, FieldVisibilityResource.Customer.class,
                            FieldVisibilityResource.Purchase.class, FieldVisibilityResource.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                    .addAsResource(
                            new StringAsset("quarkus.smallrye-graphql.schema-include-introspection-types=true\n" +
                                    "quarkus.smallrye-graphql.field-visibility=no-introspection"),
                            "application.properties"));

    @Test
    void testSchemaWithInvisibleFields() {
        given()
                .when()
                .accept(MediaType.APPLICATION_JSON)
                .get("/graphql/schema.graphql")
                .then()
                .statusCode(200)
                .and().body(containsStringButNoFields("type __Directive"))
                .and().body(containsStringButNoFields("type __EnumValue"))
                .and().body(containsStringButNoFields("type __Field"))
                .and().body(containsStringButNoFields("type __InputValue"))
                .and().body(containsStringButNoFields("type __Schema"))
                .and().body(containsStringButNoFields("type __Type"));
    }

    private org.hamcrest.Matcher<String> containsStringButNoFields(String s) {
        return new org.hamcrest.BaseMatcher<String>() {
            @Override
            public boolean matches(Object item) {
                return ((String) item).contains(s) && !((String) item).contains(s + " {");
            }

            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("a string containing ").appendValue(s)
                        .appendText(" but not containing (without fields) ").appendValue(s);
            }
        };
    }
}
