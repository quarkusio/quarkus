package io.quarkus.oidc.token.propagation.deployment.test;

import static org.hamcrest.Matchers.equalTo;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.token.propagation.AccessTokenRequestFilter;
import io.quarkus.oidc.token.propagation.common.AccessToken;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.oidc.client.OidcTestClient;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;

@QuarkusTestResource(OidcWiremockTestResource.class)
public class AccessTokenAnnotationTest {

    final static OidcTestClient client = new OidcTestClient();

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DefaultClientDefaultExchange.class, DefaultClientEnabledExchange.class,
                            NamedClientDefaultExchange.class, MultiProviderFrontendResource.class, ProtectedResource.class,
                            CustomAccessTokenRequestFilter.class)
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
                                            """),
                            "application.properties"));

    @AfterAll
    public static void close() {
        client.close();
    }

    @Test
    public void testDefaultClientEnabledTokenExchange() {
        testRestClientTokenPropagation(true, "defaultClientEnabledExchange");
    }

    @Test
    public void testDefaultClientDefaultTokenExchange() {
        testRestClientTokenPropagation(false, "defaultClientDefaultExchange");
    }

    @Test
    public void testNamedClientDefaultTokenExchange() {
        testRestClientTokenPropagation(true, "namedClientDefaultExchange");
    }

    private void testRestClientTokenPropagation(boolean exchangeEnabled, String clientKey) {
        String newTokenUsername = exchangeEnabled ? "bob" : "alice";
        RestAssured.given().auth().oauth2(getBearerAccessToken())
                .queryParam("client-key", clientKey)
                .when().get("/frontend/token-propagation")
                .then()
                .statusCode(200)
                .body(equalTo("original token username: alice new token username: " + newTokenUsername));
    }

    public String getBearerAccessToken() {
        return client.getAccessToken("alice", "alice");
    }

    @RegisterRestClient(baseUri = "http://localhost:8081/protected")
    @AccessToken
    @Path("/")
    public interface DefaultClientDefaultExchange {
        @GET
        String getUserName();
    }

    @RegisterRestClient(baseUri = "http://localhost:8081/protected")
    @io.quarkus.oidc.token.propagation.AccessToken(exchangeTokenClient = "Default")
    @Path("/")
    public interface DefaultClientEnabledExchange {
        @GET
        String getUserName();
    }

    @RegisterRestClient(baseUri = "http://localhost:8081/protected")
    @AccessToken(exchangeTokenClient = "named")
    @Path("/")
    public interface NamedClientDefaultExchange {
        @GET
        String getUserName();
    }

    // tests no AmbiguousResolutionException is raised
    @Singleton
    @Unremovable
    public static class CustomAccessTokenRequestFilter extends AccessTokenRequestFilter {
    }

    @Path("/frontend")
    public static class MultiProviderFrontendResource {
        @Inject
        @RestClient
        DefaultClientDefaultExchange defaultClientDefaultExchange;

        @Inject
        @RestClient
        DefaultClientEnabledExchange defaultClientEnabledExchange;

        @Inject
        @RestClient
        NamedClientDefaultExchange namedClientDefaultExchange;

        @Inject
        JsonWebToken jwt;

        @GET
        @Path("token-propagation")
        @RolesAllowed("admin")
        public String userNameTokenPropagation(@QueryParam("client-key") String clientKey) {
            return getResponseWithExchangedUsername(clientKey);
        }

        @GET
        @Path("token-propagation-with-augmentor")
        @RolesAllowed("tester") // tester role is granted by SecurityIdentityAugmentor
        public String userNameTokenPropagationWithSecIdentityAugmentor(@QueryParam("client-key") String clientKey) {
            return getResponseWithExchangedUsername(clientKey);
        }

        private String getResponseWithExchangedUsername(String clientKey) {
            if ("alice".equals(jwt.getName())) {
                return "original token username: " + jwt.getName() + " new token username: " + getUserName(clientKey);
            } else {
                throw new RuntimeException();
            }
        }

        private String getUserName(String clientKey) {
            return switch (clientKey) {
                case "defaultClientDefaultExchange" -> defaultClientDefaultExchange.getUserName();
                case "defaultClientEnabledExchange" -> defaultClientEnabledExchange.getUserName();
                case "namedClientDefaultExchange" -> namedClientDefaultExchange.getUserName();
                default -> throw new IllegalArgumentException("Unknown client key");
            };
        }
    }
}
