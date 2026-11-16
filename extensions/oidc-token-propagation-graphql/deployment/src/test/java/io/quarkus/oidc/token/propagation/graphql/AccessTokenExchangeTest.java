package io.quarkus.oidc.token.propagation.graphql;

import static org.hamcrest.Matchers.equalTo;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.graphql.Query;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.token.propagation.common.AccessToken;
import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.oidc.client.OidcTestClient;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.smallrye.mutiny.Uni;

@QuarkusTestResource(OidcWiremockTestResource.class)
public class AccessTokenExchangeTest {

    final static OidcTestClient client = new OidcTestClient();

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ProtectedGraphQLApi.class, ExchangeTypesafeGraphQLClient.class,
                            SameConfigKeyExchangeTypesafeGraphQLClient.class, NamedExchangeTypesafeGraphQLClient.class,
                            ExchangeFrontendResource.class, MockTokenEndpoint.class))
            .withConfiguration(
                    """
                            quarkus.oidc.auth-server-url=${keycloak.url:replaced-by-test}/realms/quarkus
                            quarkus.oidc.client-id=quarkus-app
                            quarkus.oidc.credentials.secret=secret

                            # the default OIDC client uses the JWT bearer grant, the WireMock token endpoint issues a token to bob
                            quarkus.oidc-client.auth-server-url=${quarkus.oidc.auth-server-url}
                            quarkus.oidc-client.client-id=${quarkus.oidc.client-id}
                            quarkus.oidc-client.credentials.client-secret.value=${quarkus.oidc.credentials.secret}
                            quarkus.oidc-client.credentials.client-secret.method=post
                            quarkus.oidc-client.grant.type=jwt
                            quarkus.oidc-client.scopes=https://graph.microsoft.com/user.read,offline_access
                            quarkus.oidc-client.grant-options.jwt.requested_token_use=on_behalf_of
                            quarkus.oidc-client.token-path=${keycloak.url}/realms/quarkus/jwt-bearer-token

                            # the named OIDC client uses the token exchange grant and the token endpoint mocked by the test
                            quarkus.oidc-client.named.auth-server-url=${quarkus.oidc-client.auth-server-url}
                            quarkus.oidc-client.named.client-id=${quarkus.oidc-client.client-id}
                            quarkus.oidc-client.named.credentials.client-secret.value=${quarkus.oidc-client.credentials.client-secret.value}
                            quarkus.oidc-client.named.credentials.client-secret.method=${quarkus.oidc-client.credentials.client-secret.method}
                            quarkus.oidc-client.named.grant.type=exchange
                            quarkus.oidc-client.named.token-path=http://localhost:${quarkus.http.test-port:8081}/mock-oidc/token

                            quarkus.smallrye-graphql-client.exchange-client.url=http://localhost:${quarkus.http.test-port:8081}/graphql
                            quarkus.smallrye-graphql-client.named-exchange-client.url=http://localhost:${quarkus.http.test-port:8081}/graphql
                            """);

    @Inject
    MockTokenEndpoint mockTokenEndpoint;

    @AfterAll
    public static void close() {
        client.close();
    }

    @Test
    public void testTokenExchange() {
        // Frontend is called with alice's token
        // The exchange client exchanges it to bob's token (via WireMock jwt-bearer endpoint)
        // The downstream GraphQL API returns bob, proving the exchange occurred
        assertExchangedUserName("/frontend-exchange/me", "bob");
    }

    @Test
    public void testTokenExchangeWhenClientIsAsynchronous() {
        assertExchangedUserName("/frontend-exchange/me-async", "bob");
    }

    @Test
    public void testTokenExchangeWhenClientsShareConfigKey() {
        assertExchangedUserName("/frontend-exchange/me-same-config-key", "bob");
    }

    @Test
    public void testNamedClientTokenExchange() {
        // The GraphQL client is configured to exchange the token through a named OIDC client ("named")
        // instead of the default one, only the token endpoint of the named OIDC client issues tokens to carol
        String aliceToken = client.getAccessToken("alice", "alice");
        mockTokenEndpoint.respondWithTokenIssuedTo(aliceToken, "carol");
        assertExchangedUserName("/frontend-exchange/me-named", aliceToken, "carol");
    }

    private static void assertExchangedUserName(String path, String userName) {
        assertExchangedUserName(path, client.getAccessToken("alice", "alice"), userName);
    }

    private static void assertExchangedUserName(String path, String aliceToken, String userName) {
        RestAssured.given().auth().oauth2(aliceToken)
                .when().get(path)
                .then()
                .statusCode(200)
                .body(equalTo(userName)); // not alice - proves exchange happened
    }

    @Path("/frontend-exchange")
    @Authenticated
    public static class ExchangeFrontendResource {

        @Inject
        ExchangeTypesafeGraphQLClient exchangeClient;

        @Inject
        SameConfigKeyExchangeTypesafeGraphQLClient sameConfigKeyExchangeClient;

        @Inject
        NamedExchangeTypesafeGraphQLClient namedExchangeClient;

        @GET
        @Path("/me")
        public String me() {
            return exchangeClient.me();
        }

        @GET
        @Path("/me-async")
        public Uni<String> meAsync() {
            return exchangeClient.meAsync();
        }

        @GET
        @Path("/me-same-config-key")
        public String meSameConfigKey() {
            return sameConfigKeyExchangeClient.me();
        }

        @GET
        @Path("/me-named")
        public String meNamed() {
            return namedExchangeClient.me();
        }
    }

    @GraphQLClientApi(configKey = "exchange-client")
    @AccessToken(exchangeTokenClient = "Default")
    public interface SameConfigKeyExchangeTypesafeGraphQLClient {

        @Query
        String me();
    }
}
