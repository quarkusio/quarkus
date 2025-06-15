package io.quarkus.smallrye.graphql.deployment;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.common.annotation.NonBlocking;

public class ErrorTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().withApplicationRoot(
            (jar) -> jar.addClasses(ErrorApi.class, Foo.class).addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testNonBlockingError() {
        String query = getPayload("{ foo { message} }");
        RestAssured.given().body(query).contentType(MEDIATYPE_JSON).post("/graphql/").then().assertThat()
                .statusCode(500);
    }

    @Test
    public void testBlockingError() {
        String query = getPayload("{ blockingFoo { message} }");
        RestAssured.given().body(query).contentType(MEDIATYPE_JSON).post("/graphql/").then().log().everything()
                .assertThat().statusCode(500);
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
    public static class ErrorApi {

        @Query
        @NonBlocking
        public Foo foo() {
            throw new OutOfMemoryError("a SuperHero has used all the memory");
        }

        @Query
        public Foo blockingFoo() {
            throw new OutOfMemoryError("a SuperHero has used all the memory");
        }

    }

}
