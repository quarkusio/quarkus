package io.quarkus.oidc.token.propagation.reactive.deployment.test;

import static org.hamcrest.Matchers.equalTo;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.token.propagation.common.AccessToken;
import io.quarkus.oidc.token.propagation.reactive.AccessTokenRequestReactiveFilter;
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
                            CustomAccessTokenRequestFilter.class, NamedClientDefaultExchange_OnMethod.class,
                            DefaultClientEnabledExchange_OnMethod.class, DefaultClientDefaultExchange_OnMethod.class,
                            MultipleClientsAndMultipleMethods.class, DefaultClientDefaultExchange_RepeatedAnnotation.class,
                            NamedClientDefaultExchange_RepeatedAnnotation.class,
                            DefaultClientDefaultExchangeOnClass_NamedOnMethod.class,
                            NamedClientDefaultExchangeOnClass_DefaultClientOnMethod.class)
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
        testRestClientTokenPropagation(true, "defaultClientEnabledExchange_OnMethod");
        testRestClientTokenPropagation(true, "multipleClientsAndMultipleMethods_DefaultClientEnabledExchange");
        testRestClientTokenPropagation(true,
                "namedClientDefaultExchangeOnClassDefaultClientOnMethod_methodLevelDefaultClientEnabledExchange");
        testRestClientTokenPropagation(true,
                "defaultClientDefaultExchangeOnClassNamedOnMethod_classLevelDefaultClientEnabledExchange");
    }

    @Test
    public void testDefaultClientDefaultTokenExchange() {
        testRestClientTokenPropagation(false, "defaultClientDefaultExchange");
        testRestClientTokenPropagation(false, "defaultClientDefaultExchange_RepeatedAnnotation");
        testRestClientTokenPropagation(false, "defaultClientDefaultExchangeOnClassNamedOnMethod_classLevelDefaultClient");
        testRestClientTokenPropagation(false, "defaultClientDefaultExchange_OnMethod");
        testRestClientTokenPropagation(false, "multipleClientsAndMultipleMethods_DefaultClientDefaultExchange");
        testRestClientTokenPropagation(false,
                "namedClientDefaultExchangeOnClassDefaultClientOnMethod_methodLevelDefaultClientDefaultExchange");
    }

    @Test
    public void testNamedClientDefaultTokenExchange() {
        testRestClientTokenPropagation(true, "namedClientDefaultExchange");
        testRestClientTokenPropagation(true, "namedClientDefaultExchange_OnMethod");
        testRestClientTokenPropagation(true, "multipleClientsAndMultipleMethods_NamedClientDefaultExchange");
        testRestClientTokenPropagation(true, "namedClientDefaultExchangeOnClassDefaultClientOnMethod_classLevelNamedClient");
        testRestClientTokenPropagation(true,
                "defaultClientDefaultExchangeOnClassNamedOnMethod_classLevelDefaultClientDefaultExchange");
    }

    @Test
    public void testNoTokenPropagation() {
        RestAssured.given().auth().oauth2(getBearerAccessToken())
                .queryParam("client-key", "multipleClientsAndMultipleMethods_NoAccessToken")
                .when().get("/frontend/token-propagation")
                .then()
                .statusCode(500)
                .body(Matchers.containsString("Unauthorized, status code 401"));
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
    @AccessToken(exchangeTokenClient = "named")
    @Path("/")
    public interface NamedClientDefaultExchangeOnClass_DefaultClientOnMethod {

        @GET
        String getUserName();

        @AccessToken
        @GET
        String getUserName_DefaultClientDefaultExchange();

        @AccessToken(exchangeTokenClient = "Default")
        @GET
        String getUserName_DefaultClientEnabledExchange();
    }

    @RegisterRestClient(baseUri = "http://localhost:8081/protected")
    @AccessToken
    @Path("/")
    public interface DefaultClientDefaultExchangeOnClass_NamedOnMethod {

        @GET
        String getUserName();

        @AccessToken(exchangeTokenClient = "named")
        @GET
        String getUserName_NamedClientDefaultExchange();

        @AccessToken(exchangeTokenClient = "Default")
        @GET
        String getUserName_DefaultClientEnabledExchange();
    }

    @RegisterRestClient(baseUri = "http://localhost:8081/protected")
    @AccessToken
    @Path("/")
    public interface DefaultClientDefaultExchange_RepeatedAnnotation {
        @AccessToken
        @GET
        String getUserName();
    }

    @RegisterRestClient(baseUri = "http://localhost:8081/protected")
    @AccessToken
    @Path("/")
    public interface DefaultClientDefaultExchange {
        @GET
        String getUserName();
    }

    @RegisterRestClient(baseUri = "http://localhost:8081/protected")
    @AccessToken(exchangeTokenClient = "Default")
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

    @RegisterRestClient(baseUri = "http://localhost:8081/protected")
    @Path("/")
    public interface DefaultClientDefaultExchange_OnMethod {
        @AccessToken
        @GET
        String getUserName();
    }

    @RegisterRestClient(baseUri = "http://localhost:8081/protected")
    @Path("/")
    public interface DefaultClientEnabledExchange_OnMethod {
        @AccessToken(exchangeTokenClient = "Default")
        @GET
        String getUserName();
    }

    @RegisterRestClient(baseUri = "http://localhost:8081/protected")
    @Path("/")
    public interface MultipleClientsAndMultipleMethods {

        @AccessToken
        @GET
        String getUserName_DefaultClientDefaultExchange();

        @AccessToken(exchangeTokenClient = "named")
        @GET
        String getUserName_NamedClientDefaultExchange();

        @AccessToken(exchangeTokenClient = "Default")
        @GET
        String getUserName_DefaultClientEnabledExchange();

        @GET
        String getUserName_NoAccessToken();
    }

    @RegisterRestClient(baseUri = "http://localhost:8081/protected")
    @Path("/")
    public interface NamedClientDefaultExchange_OnMethod {
        @AccessToken(exchangeTokenClient = "named")
        @GET
        String getUserName();
    }

    @AccessToken(exchangeTokenClient = "named")
    @RegisterRestClient(baseUri = "http://localhost:8081/protected")
    @Path("/")
    public interface NamedClientDefaultExchange_RepeatedAnnotation {
        @AccessToken(exchangeTokenClient = "named")
        @GET
        String getUserName();
    }

    // tests no AmbiguousResolutionException is raised
    @Singleton
    @Unremovable
    public static class CustomAccessTokenRequestFilter extends AccessTokenRequestReactiveFilter {
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
        @RestClient
        DefaultClientDefaultExchange_OnMethod defaultClientDefaultExchange_OnMethod;

        @Inject
        @RestClient
        DefaultClientEnabledExchange_OnMethod defaultClientEnabledExchange_OnMethod;

        @Inject
        @RestClient
        NamedClientDefaultExchange_OnMethod namedClientDefaultExchange_OnMethod;

        @Inject
        @RestClient
        MultipleClientsAndMultipleMethods multipleClientsAndMultipleMethods;

        @Inject
        @RestClient
        DefaultClientDefaultExchange_RepeatedAnnotation defaultClientDefaultExchange_RepeatedAnnotation;

        @Inject
        @RestClient
        NamedClientDefaultExchange_RepeatedAnnotation namedClientDefaultExchange_RepeatedAnnotation;

        @Inject
        @RestClient
        NamedClientDefaultExchangeOnClass_DefaultClientOnMethod namedClientDefaultExchangeOnClassDefaultClientOnMethod;

        @Inject
        @RestClient
        DefaultClientDefaultExchangeOnClass_NamedOnMethod defaultClientDefaultExchangeOnClassNamedOnMethod;

        @Inject
        JsonWebToken jwt;

        @ConfigProperty(name = "quarkus.oidc.auth-server-url")
        String authServerUrl;

        @ConfigProperty(name = "quarkus.oidc-client.auth-server-url")
        String clientAuthServerUrl;

        @ConfigProperty(name = "keycloak.url")
        String keycloakUrl;

        @PostConstruct
        void init() {
            System.out.println("keycloakUrl: " + keycloakUrl);
            System.out.println("authServerUrl: " + authServerUrl);
            System.out.println("clientAuthServerUrl: " + clientAuthServerUrl);
        }

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
                case "defaultClientDefaultExchange_OnMethod" -> defaultClientDefaultExchange_OnMethod.getUserName();
                case "defaultClientEnabledExchange_OnMethod" -> defaultClientEnabledExchange_OnMethod.getUserName();
                case "namedClientDefaultExchange_OnMethod" -> namedClientDefaultExchange_OnMethod.getUserName();
                case "multipleClientsAndMultipleMethods_DefaultClientDefaultExchange" ->
                    multipleClientsAndMultipleMethods.getUserName_DefaultClientDefaultExchange();
                case "multipleClientsAndMultipleMethods_DefaultClientEnabledExchange" ->
                    multipleClientsAndMultipleMethods.getUserName_DefaultClientEnabledExchange();
                case "multipleClientsAndMultipleMethods_NamedClientDefaultExchange" ->
                    multipleClientsAndMultipleMethods.getUserName_NamedClientDefaultExchange();
                case "multipleClientsAndMultipleMethods_NoAccessToken" ->
                    multipleClientsAndMultipleMethods.getUserName_NoAccessToken();
                case "defaultClientDefaultExchange_RepeatedAnnotation" ->
                    defaultClientDefaultExchange_RepeatedAnnotation.getUserName();
                case "namedClientDefaultExchange_RepeatedAnnotation" ->
                    namedClientDefaultExchange_RepeatedAnnotation.getUserName();
                case "namedClientDefaultExchangeOnClassDefaultClientOnMethod_classLevelNamedClient" ->
                    namedClientDefaultExchangeOnClassDefaultClientOnMethod.getUserName();
                case "namedClientDefaultExchangeOnClassDefaultClientOnMethod_methodLevelDefaultClientEnabledExchange" ->
                    namedClientDefaultExchangeOnClassDefaultClientOnMethod.getUserName_DefaultClientEnabledExchange();
                case "namedClientDefaultExchangeOnClassDefaultClientOnMethod_methodLevelDefaultClientDefaultExchange" ->
                    namedClientDefaultExchangeOnClassDefaultClientOnMethod.getUserName_DefaultClientDefaultExchange();
                case "defaultClientDefaultExchangeOnClassNamedOnMethod_classLevelDefaultClient" ->
                    defaultClientDefaultExchangeOnClassNamedOnMethod.getUserName();
                case "defaultClientDefaultExchangeOnClassNamedOnMethod_classLevelDefaultClientEnabledExchange" ->
                    defaultClientDefaultExchangeOnClassNamedOnMethod.getUserName_DefaultClientEnabledExchange();
                case "defaultClientDefaultExchangeOnClassNamedOnMethod_classLevelDefaultClientDefaultExchange" ->
                    defaultClientDefaultExchangeOnClassNamedOnMethod.getUserName_NamedClientDefaultExchange();
                default -> throw new IllegalArgumentException("Unknown client key");
            };
        }
    }
}
