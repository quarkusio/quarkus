package io.quarkus.oidc.token.propagation.graphql;

import static org.hamcrest.Matchers.containsString;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.graphql.Query;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.token.propagation.common.AccessToken;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.oidc.client.OidcTestClient;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;

@QuarkusTestResource(OidcWiremockTestResource.class)
class AccessTokenPropagationFailureTest {

    static final OidcTestClient client = new OidcTestClient();

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(ProtectedGraphQLApi.class, PropagationTypesafeGraphQLClient.class,
                            UnknownOidcClientGraphQLClient.class, WrongGrantTypeGraphQLClient.class,
                            PublicFrontendResource.class))
            .withConfiguration(
                    """
                            quarkus.oidc.auth-server-url=${keycloak.url:replaced-by-test}/realms/quarkus
                            quarkus.oidc.client-id=quarkus-app
                            quarkus.oidc.credentials.secret=secret

                            quarkus.oidc-client.auth-server-url=${quarkus.oidc.auth-server-url}
                            quarkus.oidc-client.client-id=${quarkus.oidc.client-id}
                            quarkus.oidc-client.credentials.secret=${quarkus.oidc.credentials.secret}

                            quarkus.smallrye-graphql-client.propagation-client.url=http://localhost:${quarkus.http.test-port:8081}/graphql
                            quarkus.smallrye-graphql-client.unknown-oidc-client.url=http://localhost:${quarkus.http.test-port:8081}/graphql
                            quarkus.smallrye-graphql-client.wrong-grant-type.url=http://localhost:${quarkus.http.test-port:8081}/graphql
                            """);

    @AfterAll
    static void close() {
        client.close();
    }

    @Test
    void testNoAccessTokenToPropagate() {
        RestAssured.when().get("/public-frontend/me")
                .then()
                .statusCode(500)
                .body(containsString("No access token is available to propagate to the GraphQL client 'propagation-client': "
                        + "the current TokenCredential has no token"));
    }

    @Test
    void testUnknownTokenExchangeOidcClient() {
        RestAssured.given().auth().oauth2(client.getAccessToken("alice", "alice"))
                .when().get("/public-frontend/unknown-oidc-client")
                .then()
                .statusCode(500)
                .body(containsString("GraphQL client 'unknown-oidc-client' requires the 'unknown' OIDC client to exchange "
                        + "the access token, but there is no such OIDC client"));
    }

    @Test
    void testTokenExchangeOidcClientWithWrongGrantType() {
        RestAssured.given().auth().oauth2(client.getAccessToken("alice", "alice"))
                .when().get("/public-frontend/wrong-grant-type")
                .then()
                .statusCode(500)
                .body(containsString("GraphQL client 'wrong-grant-type' requires the default OIDC client to exchange "
                        + "the access token, but this OIDC client is configured to use the client_credentials grant type"));
    }

    @Path("/public-frontend")
    public static class PublicFrontendResource {

        @Inject
        PropagationTypesafeGraphQLClient propagationClient;

        @Inject
        UnknownOidcClientGraphQLClient unknownOidcClient;

        @Inject
        WrongGrantTypeGraphQLClient wrongGrantTypeClient;

        @GET
        @Path("/me")
        public String me() {
            return propagationClient.me();
        }

        @GET
        @Path("/unknown-oidc-client")
        public String unknownOidcClient() {
            return unknownOidcClient.me();
        }

        @GET
        @Path("/wrong-grant-type")
        public String wrongGrantType() {
            return wrongGrantTypeClient.me();
        }
    }

    @GraphQLClientApi(configKey = "unknown-oidc-client")
    @AccessToken(exchangeTokenClient = "unknown")
    public interface UnknownOidcClientGraphQLClient {

        @Query
        String me();
    }

    @GraphQLClientApi(configKey = "wrong-grant-type")
    @AccessToken(exchangeTokenClient = "Default")
    public interface WrongGrantTypeGraphQLClient {

        @Query
        String me();
    }
}
