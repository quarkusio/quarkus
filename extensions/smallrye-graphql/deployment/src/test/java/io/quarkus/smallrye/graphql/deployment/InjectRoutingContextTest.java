package io.quarkus.smallrye.graphql.deployment;

import static org.hamcrest.Matchers.containsString;

import javax.inject.Inject;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.RoutingContext;

/**
 * Make sure that it is possible to inject and use the RoutingContext
 * when processing GraphQL queries.
 */
public class InjectRoutingContextTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(InjectRoutingContext.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void verifyRoutingContextCanBeInjected() {
        String query = getPayload("{ foo }");
        RestAssured.given()
                .body(query)
                .header("Tenant-Id", "123456")
                .contentType(MEDIATYPE_JSON)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .body(containsString("123456"));
    }

    @GraphQLApi
    public static class InjectRoutingContext {

        @Inject
        RoutingContext routingContext;

        @Query
        public String foo() {
            return routingContext.request().getHeader("Tenant-Id");
        }

    }

}
