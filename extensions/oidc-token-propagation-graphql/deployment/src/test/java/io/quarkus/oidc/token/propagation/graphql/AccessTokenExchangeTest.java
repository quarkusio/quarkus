package io.quarkus.oidc.token.propagation.graphql;

import static org.hamcrest.Matchers.equalTo;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.oidc.client.OidcTestClient;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;

@QuarkusTestResource(value = OidcWiremockTestResource.class, restrictToAnnotatedClass = true)
public class AccessTokenExchangeTest {

    final static OidcTestClient client = new OidcTestClient();

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ProtectedGraphQLApi.class, ExchangeTypesafeGraphQLClient.class,
                            NamedExchangeTypesafeGraphQLClient.class, ExchangeFrontendResource.class)
                    .addAsResource(
                            new StringAsset(
                                    """
                                            quarkus.oidc.auth-server-url=${keycloak.url:replaced-by-test}/realms/quarkus
                                            quarkus.oidc.client-id=quarkus-app
                                            quarkus.oidc.credentials.secret=secret

                                            quarkus.oidc-client.auth-server-url=${quarkus.oidc.auth-server-url}
                                            quarkus.oidc-client.client-id=${quarkus.oidc.client-id}
                                            quarkus.oidc-client.credentials.client-secret.value=${quarkus.oidc.credentials.secret}
                                            quarkus.oidc-client.credentials.client-secret.method=post
                                            quarkus.oidc-client.grant.type=jwt
                                            quarkus.oidc-client.scopes=https://graph.microsoft.com/user.read,offline_access
                                            quarkus.oidc-client.grant-options.jwt.requested_token_use=on_behalf_of
                                            quarkus.oidc-client.token-path=${keycloak.url}/realms/quarkus/jwt-bearer-token

                                            quarkus.oidc-client.named.auth-server-url=${quarkus.oidc-client.auth-server-url}
                                            quarkus.oidc-client.named.client-id=${quarkus.oidc-client.client-id}
                                            quarkus.oidc-client.named.credentials.client-secret.value=${quarkus.oidc-client.credentials.client-secret.value}
                                            quarkus.oidc-client.named.credentials.client-secret.method=${quarkus.oidc-client.credentials.client-secret.method}
                                            quarkus.oidc-client.named.grant.type=${quarkus.oidc-client.grant.type}
                                            quarkus.oidc-client.named.scopes=${quarkus.oidc-client.scopes}
                                            quarkus.oidc-client.named.grant-options.jwt.requested_token_use=${quarkus.oidc-client.grant-options.jwt.requested_token_use}
                                            quarkus.oidc-client.named.token-path=${quarkus.oidc-client.token-path}

                                            quarkus.smallrye-graphql-client.exchange-client.url=http://localhost:${quarkus.http.test-port}/graphql
                                            quarkus.smallrye-graphql-client.named-exchange-client.url=http://localhost:${quarkus.http.test-port}/graphql
                                            """),
                            "application.properties"));

    @AfterAll
    public static void close() {
        client.close();
    }

    @Test
    public void testTokenExchange() {
        // Frontend is called with alice's token
        // The exchange client exchanges it to bob's token (via WireMock jwt-bearer endpoint)
        // The downstream GraphQL API returns bob, proving the exchange occurred
        String aliceToken = client.getAccessToken("alice", "alice");
        RestAssured.given().auth().oauth2(aliceToken)
                .when().get("/frontend-exchange/me")
                .then()
                .statusCode(200)
                .body(equalTo("bob")); // bob, not alice - proves exchange happened
    }

    @Test
    public void testNamedClientTokenExchange() {
        // Same as above, but the GraphQL client is configured to exchange the token through
        // a named OIDC client ("named") instead of the default one
        String aliceToken = client.getAccessToken("alice", "alice");
        RestAssured.given().auth().oauth2(aliceToken)
                .when().get("/frontend-exchange/me-named")
                .then()
                .statusCode(200)
                .body(equalTo("bob")); // bob, not alice - proves exchange happened
    }

    @Path("/frontend-exchange")
    @Authenticated
    public static class ExchangeFrontendResource {

        @Inject
        ExchangeTypesafeGraphQLClient exchangeClient;

        @Inject
        NamedExchangeTypesafeGraphQLClient namedExchangeClient;

        @GET
        @Path("/me")
        public String me() {
            return exchangeClient.me();
        }

        @GET
        @Path("/me-named")
        public String meNamed() {
            return namedExchangeClient.me();
        }
    }
}
