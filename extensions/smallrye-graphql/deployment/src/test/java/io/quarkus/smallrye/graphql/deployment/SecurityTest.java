package io.quarkus.smallrye.graphql.deployment;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.Header;

public class SecurityTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SecuredApi.class, Foo.class)
                    .addAsResource("application-secured.properties", "application.properties")
                    .addAsResource("users.properties")
                    .addAsResource("roles.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testAuthenticatedUser() {
        String query = getPayload("{ foo { message} }");
        RestAssured.given()
                .header(new Header("Authorization", "Basic ZGF2aWQ6cXdlcnR5MTIz"))
                .body(query)
                .contentType(MEDIATYPE_JSON)
                .post("/graphql/")
                .then()
                .assertThat()
                .body("errors", nullValue())
                .body("data.foo.message", equalTo("foo"));
    }

    @Test
    public void testAuthenticatedUserWithSource() {
        String query = getPayload("{ foo { bonusFoo } }");
        RestAssured.given()
                .header(new Header("Authorization", "Basic ZGF2aWQ6cXdlcnR5MTIz"))
                .body(query)
                .contentType(MEDIATYPE_JSON)
                .post("/graphql/")
                .then()
                .assertThat()
                .body("errors", nullValue())
                .body("data.foo.bonusFoo", equalTo("bonus"));
    }

    @Test
    public void testUnauthorizedRole() {
        String query = getPayload("{ bar { message } }");
        RestAssured.given()
                .header(new Header("Authorization", "Basic ZGF2aWQ6cXdlcnR5MTIz"))
                .body(query)
                .contentType(MEDIATYPE_JSON)
                .post("/graphql")
                .then()
                .assertThat()
                .body("errors", notNullValue())
                .body("data.bar.message", nullValue());
    }

    /**
     * Call a query that we are authorized to call, but within that, retrieve a source field that we're not authorized
     * to retrieve.
     */
    @Test
    public void testUnauthorizedForSource() {
        String query = getPayload("{ foo { bonusBar } }");
        RestAssured.given()
                .header(new Header("Authorization", "Basic ZGF2aWQ6cXdlcnR5MTIz"))
                .body(query)
                .contentType(MEDIATYPE_JSON)
                .post("/graphql/")
                .then()
                .assertThat()
                .body("errors", notNullValue())
                .body("data.foo.bonusBar", nullValue());
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
    @ApplicationScoped
    public static class SecuredApi {

        @Query
        @RolesAllowed("fooRole")
        public Foo foo() {
            return new Foo("foo");
        }

        @Name("bonusFoo")
        @RolesAllowed("fooRole")
        public List<String> bonusFoo(@Source List<Foo> foos) {
            return foos.stream().map(foo -> "bonus").collect(Collectors.toList());
        }

        @Query
        @RolesAllowed("barRole")
        public Foo bar() {
            return new Foo("bar");
        }

        @Name("bonusBar")
        @RolesAllowed("barRole")
        public List<String> bonusBar(@Source List<Foo> foos) {
            return foos.stream().map(foo -> "bonus").collect(Collectors.toList());
        }

    }

}
