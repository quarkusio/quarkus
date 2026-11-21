package io.quarkus.oidc.token.propagation.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.graphql.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.token.propagation.common.AccessToken;
import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;

@QuarkusTestResource(OidcWiremockTestResource.class)
class ExchangedTokenCacheTest {

    private static final String DEFAULT_OIDC_CLIENT = "default";
    private static final String NO_CACHE_OIDC_CLIENT = "no-cache";

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(Frontend.class, ProtectedGraphQLApi.class, MockTokenEndpoint.class,
                            DefaultOidcClientGraphQLClient.class, NoCacheOidcClientGraphQLClient.class))
            .withConfiguration(
                    """
                            quarkus.oidc.auth-server-url=${keycloak.url:replaced-by-tests}/realms/quarkus
                            quarkus.oidc.client-id=quarkus-app
                            quarkus.oidc.credentials.secret=secret
                            quarkus.oidc.token.audience=any

                            quarkus.oidc-client.auth-server-url=${quarkus.oidc.auth-server-url}
                            quarkus.oidc-client.client-id=${quarkus.oidc.client-id}
                            quarkus.oidc-client.credentials.client-secret.value=${quarkus.oidc.credentials.secret}
                            quarkus.oidc-client.credentials.client-secret.method=post
                            quarkus.oidc-client.grant.type=exchange
                            quarkus.oidc-client.grant-options.exchange.audience=quarkus-app-exchange
                            quarkus.oidc-client.token-path=http://localhost:${quarkus.http.test-port:8081}/mock-oidc/token

                            quarkus.oidc-client.no-cache.auth-server-url=${quarkus.oidc-client.auth-server-url}
                            quarkus.oidc-client.no-cache.client-id=${quarkus.oidc-client.client-id}
                            quarkus.oidc-client.no-cache.credentials.client-secret.value=${quarkus.oidc-client.credentials.client-secret.value}
                            quarkus.oidc-client.no-cache.credentials.client-secret.method=post
                            quarkus.oidc-client.no-cache.grant.type=exchange
                            quarkus.oidc-client.no-cache.grant-options.exchange.audience=quarkus-app-exchange
                            quarkus.oidc-client.no-cache.token-path=${quarkus.oidc-client.token-path}
                            quarkus.oidc-client.no-cache.token-cache-max-size=0

                            quarkus.smallrye-graphql-client.default-oidc-client.url=http://localhost:${quarkus.http.test-port:8081}/graphql
                            quarkus.smallrye-graphql-client.no-cache-oidc-client.url=http://localhost:${quarkus.http.test-port:8081}/graphql
                            """);

    @Inject
    MockTokenEndpoint mockTokenEndpoint;

    @Test
    void testExchangedTokenIsReusedForSameIncomingToken() {
        String token = tokenIssuedToAlice("same-token");
        mockTokenEndpoint.respondWithTokenIssuedTo(token, "bob");

        assertExchangedUserName(DEFAULT_OIDC_CLIENT, token, "bob");
        assertExchangedUserName(DEFAULT_OIDC_CLIENT, token, "bob");

        assertThat(mockTokenEndpoint.exchangeCount(token)).isEqualTo(1);
    }

    @Test
    void testTokensAreExchangedPerIncomingToken() {
        String firstToken = tokenIssuedToAlice("first-token");
        String secondToken = tokenIssuedToAlice("second-token");
        mockTokenEndpoint.respondWithTokenIssuedTo(firstToken, "bob");
        mockTokenEndpoint.respondWithTokenIssuedTo(secondToken, "carol");

        assertExchangedUserName(DEFAULT_OIDC_CLIENT, firstToken, "bob");
        assertExchangedUserName(DEFAULT_OIDC_CLIENT, secondToken, "carol");
        assertExchangedUserName(DEFAULT_OIDC_CLIENT, firstToken, "bob");
        assertExchangedUserName(DEFAULT_OIDC_CLIENT, secondToken, "carol");

        assertThat(mockTokenEndpoint.exchangeCount(firstToken)).isEqualTo(1);
        assertThat(mockTokenEndpoint.exchangeCount(secondToken)).isEqualTo(1);
    }

    @Test
    void testTokenIsExchangedOnEveryRequestWhenCacheIsDisabled() {
        String token = tokenIssuedToAlice("uncached-token");
        mockTokenEndpoint.respondWithTokenIssuedTo(token, "bob");

        assertExchangedUserName(NO_CACHE_OIDC_CLIENT, token, "bob");
        assertExchangedUserName(NO_CACHE_OIDC_CLIENT, token, "bob");

        assertThat(mockTokenEndpoint.exchangeCount(token)).isEqualTo(2);
    }

    @Test
    void testExchangedTokenIsNotPropagatedWhenItIsMissing() {
        String token = tokenIssuedToAlice("no-exchanged-token");
        mockTokenEndpoint.respondWithoutAccessToken(token);

        RestAssured.given().auth().oauth2(token)
                .queryParam("oidc-client", DEFAULT_OIDC_CLIENT)
                .when().get("/frontend/exchanged-user-name")
                .then()
                .statusCode(500)
                .body(containsString("GraphQL client 'default-oidc-client' cannot propagate the exchanged access token: "
                        + "the default OIDC client did not return it"));
    }

    private static String tokenIssuedToAlice(String subject) {
        return OidcWiremockTestResource.generateJwtToken("alice", Set.of("admin"), subject);
    }

    private static void assertExchangedUserName(String oidcClient, String token, String userName) {
        RestAssured.given().auth().oauth2(token)
                .queryParam("oidc-client", oidcClient)
                .when().get("/frontend/exchanged-user-name")
                .then()
                .statusCode(200)
                .body(equalTo(userName));
    }

    @Path("/frontend")
    @Authenticated
    public static class Frontend {

        @Inject
        DefaultOidcClientGraphQLClient defaultOidcClientGraphQLClient;

        @Inject
        NoCacheOidcClientGraphQLClient noCacheOidcClientGraphQLClient;

        @GET
        @Path("exchanged-user-name")
        public String exchangedUserName(@QueryParam("oidc-client") String oidcClient) {
            return switch (oidcClient) {
                case DEFAULT_OIDC_CLIENT -> defaultOidcClientGraphQLClient.me();
                case NO_CACHE_OIDC_CLIENT -> noCacheOidcClientGraphQLClient.me();
                default -> throw new IllegalArgumentException("Unknown OIDC client " + oidcClient);
            };
        }
    }

    @GraphQLClientApi(configKey = "default-oidc-client")
    @AccessToken(exchangeTokenClient = "Default")
    public interface DefaultOidcClientGraphQLClient {

        @Query
        String me();
    }

    @GraphQLClientApi(configKey = "no-cache-oidc-client")
    @AccessToken(exchangeTokenClient = NO_CACHE_OIDC_CLIENT)
    public interface NoCacheOidcClientGraphQLClient {

        @Query
        String me();
    }
}
