package io.quarkus.oidc.token.propagation.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import java.time.Duration;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.token.propagation.common.AccessToken;
import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;

@QuarkusTestResource(OidcWiremockTestResource.class)
class ExchangedTokenCacheTest {

    private static final String DEFAULT_OIDC_CLIENT = "default";
    private static final String SMALL_CACHE_OIDC_CLIENT = "small-cache";
    private static final String NO_CACHE_OIDC_CLIENT = "no-cache";

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(Frontend.class, ProtectedResource.class, MockTokenEndpoint.class,
                            DefaultOidcClientService.class, SmallCacheOidcClientService.class,
                            NoCacheOidcClientService.class))
            .withConfiguration(
                    """
                            quarkus.oidc.auth-server-url=${keycloak.url:replaced-by-tests}/realms/quarkus
                            quarkus.oidc.client-id=quarkus-app
                            quarkus.oidc.credentials.secret=secret

                            quarkus.oidc-client.auth-server-url=${quarkus.oidc.auth-server-url}
                            quarkus.oidc-client.client-id=${quarkus.oidc.client-id}
                            quarkus.oidc-client.credentials.client-secret.value=${quarkus.oidc.credentials.secret}
                            quarkus.oidc-client.credentials.client-secret.method=post
                            quarkus.oidc-client.grant.type=exchange
                            quarkus.oidc-client.grant-options.exchange.audience=quarkus-app-exchange
                            quarkus.oidc-client.token-path=http://localhost:8081/mock-oidc/token

                            quarkus.oidc-client.small-cache.auth-server-url=${quarkus.oidc-client.auth-server-url}
                            quarkus.oidc-client.small-cache.client-id=${quarkus.oidc-client.client-id}
                            quarkus.oidc-client.small-cache.credentials.client-secret.value=${quarkus.oidc-client.credentials.client-secret.value}
                            quarkus.oidc-client.small-cache.credentials.client-secret.method=post
                            quarkus.oidc-client.small-cache.grant.type=exchange
                            quarkus.oidc-client.small-cache.grant-options.exchange.audience=quarkus-app-exchange
                            quarkus.oidc-client.small-cache.token-path=${quarkus.oidc-client.token-path}
                            quarkus.oidc-client.small-cache.token-cache-max-size=2

                            quarkus.oidc-client.no-cache.auth-server-url=${quarkus.oidc-client.auth-server-url}
                            quarkus.oidc-client.no-cache.client-id=${quarkus.oidc-client.client-id}
                            quarkus.oidc-client.no-cache.credentials.client-secret.value=${quarkus.oidc-client.credentials.client-secret.value}
                            quarkus.oidc-client.no-cache.credentials.client-secret.method=post
                            quarkus.oidc-client.no-cache.grant.type=exchange
                            quarkus.oidc-client.no-cache.grant-options.exchange.audience=quarkus-app-exchange
                            quarkus.oidc-client.no-cache.token-path=${quarkus.oidc-client.token-path}
                            quarkus.oidc-client.no-cache.token-cache-max-size=0

                            quarkus.resteasy-client-oidc-token-propagation.exchange-token=true
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
    void testExpiredExchangedTokenIsExchangedAgain() {
        String token = tokenIssuedToAlice("expiring-token");
        mockTokenEndpoint.respondWithTokenIssuedTo(token, "bob", 2);

        assertExchangedUserName(DEFAULT_OIDC_CLIENT, token, "bob");
        assertExchangedUserName(DEFAULT_OIDC_CLIENT, token, "bob");
        assertThat(mockTokenEndpoint.exchangeCount(token)).isEqualTo(1);

        mockTokenEndpoint.respondWithTokenIssuedTo(token, "carol");
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertExchangedUserName(DEFAULT_OIDC_CLIENT, token, "carol"));
        assertThat(mockTokenEndpoint.exchangeCount(token)).isEqualTo(2);
    }

    @Test
    void testFailedExchangeIsNotCached() {
        String token = tokenIssuedToAlice("failing-token");
        mockTokenEndpoint.respondWithError(token);

        RestAssured.given().auth().oauth2(token)
                .queryParam("oidc-client", DEFAULT_OIDC_CLIENT)
                .when().get("/frontend/exchanged-user-name")
                .then()
                .statusCode(500);

        mockTokenEndpoint.respondWithTokenIssuedTo(token, "bob");
        assertExchangedUserName(DEFAULT_OIDC_CLIENT, token, "bob");
        assertThat(mockTokenEndpoint.exchangeCount(token)).isEqualTo(2);
    }

    @Test
    void testLeastRecentlyUsedExchangedTokenIsEvictedWhenCacheIsFull() {
        String firstToken = tokenIssuedToAlice("first-cached-token");
        String secondToken = tokenIssuedToAlice("second-cached-token");
        String thirdToken = tokenIssuedToAlice("third-cached-token");
        mockTokenEndpoint.respondWithTokenIssuedTo(firstToken, "bob");
        mockTokenEndpoint.respondWithTokenIssuedTo(secondToken, "bob");
        mockTokenEndpoint.respondWithTokenIssuedTo(thirdToken, "bob");

        assertExchangedUserName(SMALL_CACHE_OIDC_CLIENT, firstToken, "bob");
        assertExchangedUserName(SMALL_CACHE_OIDC_CLIENT, secondToken, "bob");
        assertExchangedUserName(SMALL_CACHE_OIDC_CLIENT, thirdToken, "bob");
        assertThat(mockTokenEndpoint.exchangeCount(firstToken)).isEqualTo(1);
        assertThat(mockTokenEndpoint.exchangeCount(secondToken)).isEqualTo(1);
        assertThat(mockTokenEndpoint.exchangeCount(thirdToken)).isEqualTo(1);

        assertExchangedUserName(SMALL_CACHE_OIDC_CLIENT, thirdToken, "bob");
        assertThat(mockTokenEndpoint.exchangeCount(thirdToken)).isEqualTo(1);

        assertExchangedUserName(SMALL_CACHE_OIDC_CLIENT, firstToken, "bob");
        assertThat(mockTokenEndpoint.exchangeCount(firstToken)).isEqualTo(2);
    }

    @Test
    void testTokenIsExchangedOnEveryRequestWhenCacheIsDisabled() {
        String token = tokenIssuedToAlice("uncached-token");
        mockTokenEndpoint.respondWithTokenIssuedTo(token, "bob");

        assertExchangedUserName(NO_CACHE_OIDC_CLIENT, token, "bob");
        assertExchangedUserName(NO_CACHE_OIDC_CLIENT, token, "bob");

        assertThat(mockTokenEndpoint.exchangeCount(token)).isEqualTo(2);
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
        @RestClient
        DefaultOidcClientService defaultOidcClientService;

        @Inject
        @RestClient
        SmallCacheOidcClientService smallCacheOidcClientService;

        @Inject
        @RestClient
        NoCacheOidcClientService noCacheOidcClientService;

        @GET
        @Path("exchanged-user-name")
        public String exchangedUserName(@QueryParam("oidc-client") String oidcClient) {
            return switch (oidcClient) {
                case DEFAULT_OIDC_CLIENT -> defaultOidcClientService.getUserName();
                case SMALL_CACHE_OIDC_CLIENT -> smallCacheOidcClientService.getUserName();
                case NO_CACHE_OIDC_CLIENT -> noCacheOidcClientService.getUserName();
                default -> throw new IllegalArgumentException("Unknown OIDC client " + oidcClient);
            };
        }
    }

    @RegisterRestClient(baseUri = "http://localhost:8081/protected")
    @AccessToken
    @Path("/")
    public interface DefaultOidcClientService {
        @GET
        String getUserName();
    }

    @RegisterRestClient(baseUri = "http://localhost:8081/protected")
    @AccessToken(exchangeTokenClient = SMALL_CACHE_OIDC_CLIENT)
    @Path("/")
    public interface SmallCacheOidcClientService {
        @GET
        String getUserName();
    }

    @RegisterRestClient(baseUri = "http://localhost:8081/protected")
    @AccessToken(exchangeTokenClient = NO_CACHE_OIDC_CLIENT)
    @Path("/")
    public interface NoCacheOidcClientService {
        @GET
        String getUserName();
    }
}
