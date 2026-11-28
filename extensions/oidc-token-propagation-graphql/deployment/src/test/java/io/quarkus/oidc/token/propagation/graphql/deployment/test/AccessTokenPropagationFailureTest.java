package io.quarkus.oidc.token.propagation.graphql.deployment.test;

import static org.hamcrest.Matchers.containsString;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;

@QuarkusTestResource(OidcWiremockTestResource.class)
class AccessTokenPropagationFailureTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(ProtectedGraphQLApi.class, PropagationTypesafeGraphQLClient.class,
                            PublicFrontendResource.class))
            .withConfiguration(
                    """
                            quarkus.oidc.auth-server-url=${keycloak.url:replaced-by-test}/realms/quarkus
                            quarkus.oidc.client-id=quarkus-app
                            quarkus.oidc.credentials.secret=secret
                            quarkus.smallrye-graphql-client.propagation-client.url=http://localhost:${quarkus.http.test-port:8081}/graphql
                            """);

    @Test
    void testNoAccessTokenToPropagate() {
        RestAssured.when().get("/public-frontend/me")
                .then()
                .statusCode(500)
                .body(containsString("No access token is available to propagate to the GraphQL client 'propagation-client': "
                        + "the current TokenCredential has no token"));
    }

    @Path("/public-frontend")
    public static class PublicFrontendResource {

        @Inject
        PropagationTypesafeGraphQLClient propagationClient;

        @GET
        @Path("/me")
        public String me() {
            return propagationClient.me();
        }
    }
}
