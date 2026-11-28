package io.quarkus.oidc.token.propagation.graphql.deployment.test;

import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.graphql.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;

/**
 * Tests the access token propagation when the extensions which also handle the
 * {@link io.quarkus.oidc.token.propagation.common.AccessToken} annotation or the GraphQL clients are present.
 */
@QuarkusTestResource(OidcWiremockTestResource.class)
public class AccessTokenPropagationWithOtherExtensionsTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            // these extensions are only added to this test
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-oidc-client-graphql-deployment", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-rest-client-oidc-token-propagation-deployment",
                            Version.getVersion())))
            .withApplicationRoot((jar) -> jar
                    .addClasses(ProtectedGraphQLApi.class, PropagationTypesafeGraphQLClient.class,
                            OidcClientTypesafeGraphQLClient.class, OtherExtensionsFrontendResource.class))
            .withConfiguration(
                    """
                            quarkus.oidc.auth-server-url=${keycloak.url:replaced-by-test}/realms/quarkus
                            quarkus.oidc.client-id=quarkus-app
                            quarkus.oidc.credentials.secret=secret

                            # the WireMock token endpoint issues a token to alice
                            quarkus.oidc-client.auth-server-url=${quarkus.oidc.auth-server-url}
                            quarkus.oidc-client.client-id=${quarkus.oidc.client-id}
                            quarkus.oidc-client.credentials.secret=${quarkus.oidc.credentials.secret}

                            quarkus.smallrye-graphql-client.propagation-client.url=http://localhost:${quarkus.http.test-port:8081}/graphql
                            quarkus.smallrye-graphql-client.oidc-client.url=http://localhost:${quarkus.http.test-port:8081}/graphql
                            """);

    @Test
    public void testAccessTokenIsPropagated() {
        RestAssured.given().auth().oauth2(tokenIssuedToBob())
                .when().get("/other-extensions-frontend/access-token")
                .then()
                .statusCode(200)
                .body(equalTo("bob"));
    }

    @Test
    public void testOidcClientIsUsedWhenAccessTokenIsNotPropagated() {
        RestAssured.given().auth().oauth2(tokenIssuedToBob())
                .when().get("/other-extensions-frontend/oidc-client")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }

    private static String tokenIssuedToBob() {
        return OidcWiremockTestResource.getAccessToken("bob", Set.of("user"));
    }

    @Path("/other-extensions-frontend")
    public static class OtherExtensionsFrontendResource {

        @Inject
        PropagationTypesafeGraphQLClient accessTokenClient;

        @Inject
        OidcClientTypesafeGraphQLClient oidcClient;

        @GET
        @Path("/access-token")
        public String accessToken() {
            return accessTokenClient.me();
        }

        @GET
        @Path("/oidc-client")
        public String oidcClient() {
            return oidcClient.me();
        }
    }

    @GraphQLClientApi(configKey = "oidc-client")
    public interface OidcClientTypesafeGraphQLClient {

        @Query
        String me();
    }
}
