package io.quarkus.smallrye.graphql.deployment;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.Header;

public class SecurityTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(SecuredApi.class)
                    .addAsResource("application-secured.properties", "application.properties")
                    .addAsResource("users.properties")
                    .addAsResource("roles.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testAuthenticatedUser() {
        String query = getPayload("{ foo }");
        RestAssured.given()
                .header(new Header("Authorization", "Basic ZGF2aWQ6cXdlcnR5MTIz"))
                .body(query)
                .contentType(MEDIATYPE_JSON)
                .post("/graphql/")
                .then()
                .assertThat()
                .body("errors", nullValue())
                .body("data.foo", equalTo("foo"));
    }

    @Test
    public void testUnauthorizedRole() {
        String query = getPayload("{ bar }");
        RestAssured.given()
                .header(new Header("Authorization", "Basic ZGF2aWQ6cXdlcnR5MTIz"))
                .body(query)
                .contentType(MEDIATYPE_JSON)
                .post("/graphql")
                .then()
                .assertThat()
                .body("errors", notNullValue())
                .body("data.bar", nullValue());
    }

    @GraphQLApi
    @ApplicationScoped
    public static class SecuredApi {

        @Query
        @RolesAllowed("fooRole")
        public String foo() {
            return "foo";
        }

        @Query
        @RolesAllowed("barRole")
        public String bar() {
            return "bar";
        }

    }

}
