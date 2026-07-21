package io.quarkus.smallrye.graphql.deployment;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.smallrye.common.annotation.NonBlocking;

public class DefaultQueryDepthTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FooApi.class, Foo.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testQueryExceedingDefaultDepthIsRejected() {
        String query = getPayload(nestedQuery(11));
        RestAssured.given()
                .body(query)
                .contentType(MEDIATYPE_JSON)
                .post("/graphql/")
                .then()
                .assertThat()
                .body("errors[0].message", equalTo("maximum query depth exceeded 11 > 10"))
                .body("data", nullValue());
    }

    @Test
    public void testQueryWithinDefaultDepthIsAllowed() {
        String query = getPayload(nestedQuery(10));
        RestAssured.given()
                .body(query)
                .contentType(MEDIATYPE_JSON)
                .post("/graphql/")
                .then()
                .assertThat()
                .statusCode(200)
                .body("errors", nullValue())
                .body("data", notNullValue());
    }

    private static String nestedQuery(int depth) {
        StringBuilder sb = new StringBuilder("{ foo ");
        for (int i = 0; i < depth - 2; i++) {
            sb.append("{ nestedFoo ");
        }
        sb.append("{ message }");
        for (int i = 0; i < depth - 2; i++) {
            sb.append(" }");
        }
        sb.append(" }");
        return sb.toString();
    }

    public static class Foo {

        private String message;

        public Foo(String foo) {
            this.message = foo;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    @GraphQLApi
    public static class FooApi {

        @Query
        @NonBlocking
        public Foo foo() {
            return new Foo("foo");
        }

        public Foo nestedFoo(@Source Foo foo) {
            return new Foo("foo");
        }
    }
}
