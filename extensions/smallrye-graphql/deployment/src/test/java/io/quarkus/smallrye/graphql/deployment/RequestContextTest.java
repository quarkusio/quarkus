package io.quarkus.smallrye.graphql.deployment;

import static org.hamcrest.Matchers.containsString;

import javax.enterprise.context.ContextNotActiveException;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Make sure that the request context is active while processing GraphQL API methods.
 * This is necessary to get frameworks like JPA working.
 */
public class RequestContextTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(RequestContextApi.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void verifyRequestContextActive() {
        String query = getPayload("{ foo }");
        RestAssured.given()
                .body(query)
                .contentType(MEDIATYPE_JSON)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .body(containsString("success"));
    }

    @GraphQLApi
    public static class RequestContextApi {

        @Query
        public String foo() {
            if (!Arc.container().requestContext().isActive()) {
                throw new ContextNotActiveException();
            }
            return "success";
        }

    }

}
