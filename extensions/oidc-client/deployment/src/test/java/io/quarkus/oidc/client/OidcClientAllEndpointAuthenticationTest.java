package io.quarkus.oidc.client;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.client.runtime.OidcClientConfig;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig;
import io.quarkus.test.QuarkusExtensionTest;

class OidcClientAllEndpointAuthenticationTest {

    private static final String CLIENT_ID = "test-client";
    private static final String CLIENT_SECRET = "test-secret";

    private static final String JWT_HEADER = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0";
    private static final String BEARER_TOKEN = JWT_HEADER
            + ".eyJleHAiOjk5OTk5OTk5OTksInN1YiI6InRlc3Qtc2VydmljZSJ9.sig";
    private static final File TOKEN_DIR = new File("target/test-tokens");
    private static final File BEARER_TOKEN_FILE = new File(TOKEN_DIR, "bearer.jwt");

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    TokenResource.class,
                    MockDiscoveryEndpoint.class,
                    MockTokenEndpoint.class))
            .withConfiguration("""
                    quarkus.keycloak.devservices.enabled=false
                    quarkus.oidc.enabled=false
                    """)
            .withRuntimeConfiguration("""
                    quarkus.oidc-client.auth-server-url=http://localhost:8081/mock-oidc/basic
                    quarkus.oidc-client.client-id=%1$s
                    quarkus.oidc-client.credentials.secret=%2$s
                    quarkus.oidc-client.credentials.for-all-endpoints=true
                    quarkus.oidc-client.grant.type=client

                    quarkus.oidc-client.bearer.auth-server-url=http://localhost:8081/mock-oidc/bearer
                    quarkus.oidc-client.bearer.client-id=%1$s
                    quarkus.oidc-client.bearer.credentials.jwt.source=bearer
                    quarkus.oidc-client.bearer.credentials.jwt.token-path=%3$s
                    quarkus.oidc-client.bearer.credentials.for-all-endpoints=true
                    quarkus.oidc-client.bearer.grant.type=client

                    """.formatted(CLIENT_ID, CLIENT_SECRET,
                    BEARER_TOKEN_FILE.getAbsolutePath()))
            .setBeforeAllCustomizer(() -> {
                try {
                    Files.createDirectories(TOKEN_DIR.toPath());
                    Files.writeString(BEARER_TOKEN_FILE.toPath(), BEARER_TOKEN);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

    @Test
    void testBasicAuth() {
        testAuth("default");
    }

    @Test
    void testBearerAuth() {
        testAuth("bearer");
    }

    @Test
    void testIncompatibleMethodThrowsValidationError() {
        given()
                .get("/token/invalid-post-method")
                .then()
                .statusCode(500)
                .body(Matchers.containsString("Client credentials cannot be sent to all OIDC endpoints"));
    }

    private static void testAuth(String clientName) {
        given()
                .get("/token/" + clientName)
                .then()
                .statusCode(200)
                .body(is("test-access-token"));
    }

    @Path("/token/{clientName}")
    public static class TokenResource {

        @Inject
        OidcClients oidcClients;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String getToken(@PathParam("clientName") String clientName) {
            if ("invalid-post-method".equals(clientName)) {
                return createClientWithIncompatibleMethod();
            }
            OidcClient client = "default".equals(clientName)
                    ? oidcClients.getClient()
                    : oidcClients.getClient(clientName);
            return client.getTokens().await().indefinitely().getAccessToken();
        }

        private String createClientWithIncompatibleMethod() {
            var config = OidcClientConfig.builder()
                    .id("post-method-test")
                    .authServerUrl("http://localhost:8081/mock-oidc/basic")
                    .clientId(CLIENT_ID)
                    .credentials()
                    .clientSecret("secret", OidcClientCommonConfig.Credentials.Secret.Method.POST)
                    .forAllEndpoints()
                    .end()
                    .grant().type(OidcClientConfig.Grant.Type.CLIENT).end()
                    .build();
            oidcClients.newClient(config).await().indefinitely();
            return "wrong - config validation did not fail";
        }
    }

    @Path("/mock-oidc/{client}/.well-known/openid-configuration")
    public static class MockDiscoveryEndpoint {

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response discover(@PathParam("client") String client,
                @HeaderParam("Authorization") String authorization) {
            if (isAnonymous(client, authorization)) {
                return Response.status(401).build();
            }
            return Response.ok("""
                    {
                        "issuer": "http://localhost:8081/mock-oidc/%1$s",
                        "token_endpoint": "http://localhost:8081/mock-oidc/%1$s/token",
                        "subject_types_supported": ["public"]
                    }
                    """.formatted(client)).build();
        }
    }

    @Path("/mock-oidc/{client}/token")
    public static class MockTokenEndpoint {

        @POST
        @Produces(MediaType.APPLICATION_JSON)
        public String token() {
            return """
                    {
                        "access_token": "test-access-token",
                        "token_type": "Bearer",
                        "expires_in": 300
                    }
                    """;
        }
    }

    private static boolean isAnonymous(String client, String authorization) {
        return !switch (client) {
            case "basic" -> isBasicAuth(authorization);
            case "bearer" -> isBearerAuth(authorization);
            default -> false;
        };
    }

    private static boolean isBasicAuth(String authorization) {
        if (authorization == null || !authorization.startsWith("Basic ")) {
            return false;
        }
        String decoded = new String(Base64.getDecoder().decode(authorization.substring(6)), StandardCharsets.UTF_8);
        return (CLIENT_ID + ":" + CLIENT_SECRET).equals(decoded);
    }

    private static boolean isBearerAuth(String authorization) {
        return authorization != null && authorization.equals("Bearer " + BEARER_TOKEN);
    }

}
