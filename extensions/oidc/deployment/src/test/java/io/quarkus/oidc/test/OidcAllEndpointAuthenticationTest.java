package io.quarkus.oidc.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;

import jakarta.enterprise.context.ApplicationScoped;
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

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig;
import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

class OidcAllEndpointAuthenticationTest {

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
                    ProtectedResource.class,
                    IncompatibleMethodTenantConfigResolver.class,
                    MockDiscoveryEndpoint.class,
                    MockJwksEndpoint.class,
                    MockIntrospectionEndpoint.class))
            .withConfiguration("""
                    quarkus.keycloak.devservices.enabled=false
                    quarkus.http.auth.proactive=false
                    """)
            .withRuntimeConfiguration("""
                    quarkus.oidc.auth-server-url=http://localhost:8081/mock-oidc/basic
                    quarkus.oidc.client-id=%1$s
                    quarkus.oidc.credentials.secret=%2$s
                    quarkus.oidc.credentials.for-all-endpoints=true
                    quarkus.oidc.tenant-paths=/basic/*

                    quarkus.oidc.bearer.auth-server-url=http://localhost:8081/mock-oidc/bearer
                    quarkus.oidc.bearer.client-id=%1$s
                    quarkus.oidc.bearer.credentials.jwt.source=bearer
                    quarkus.oidc.bearer.credentials.jwt.token-path=%3$s
                    quarkus.oidc.bearer.credentials.for-all-endpoints=true
                    quarkus.oidc.bearer.tenant-paths=/bearer/*

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
        testAuth("basic");
    }

    @Test
    void testBearerAuth() {
        testAuth("bearer");
    }

    @Test
    void testIncompatibleMethodThrowsValidationError() {
        given()
                .auth().oauth2("any-token")
                .get("/post-method/protected")
                .then()
                .statusCode(500)
                .body(Matchers.containsString("Client credentials cannot be sent to all OIDC endpoints"));
    }

    private static void testAuth(String tenant) {
        given()
                .auth().oauth2("any-token")
                .get("/" + tenant + "/protected")
                .then()
                .statusCode(200)
                .body(is("OK"));
    }

    @Path("/{tenant}/protected")
    @Authenticated
    public static class ProtectedResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return "OK";
        }
    }

    @Path("/mock-oidc/{tenant}/.well-known/openid-configuration")
    public static class MockDiscoveryEndpoint {

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response discover(@PathParam("tenant") String tenant,
                @HeaderParam("Authorization") String authorization) {
            if (isAnonymous(tenant, authorization)) {
                return Response.status(401).build();
            }
            String json = """
                    {
                        "issuer": "http://localhost:8081/mock-oidc/%1$s",
                        "jwks_uri": "http://localhost:8081/mock-oidc/%1$s/protocol/openid-connect/certs",
                        "token_endpoint": "http://localhost:8081/mock-oidc/%1$s/token",
                        "introspection_endpoint": "http://localhost:8081/mock-oidc/%1$s/introspect",
                        "authorization_endpoint": "http://localhost:8081/mock-oidc/%1$s/authorize",
                        "subject_types_supported": ["public"]
                    }
                    """.formatted(tenant);
            return Response.ok(json).build();
        }
    }

    @Path("/mock-oidc/{tenant}/protocol/openid-connect/certs")
    public static class MockJwksEndpoint {

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response jwks(@PathParam("tenant") String tenant,
                @HeaderParam("Authorization") String authorization) {
            if (isAnonymous(tenant, authorization)) {
                return Response.status(401).build();
            }
            return Response.ok("{\"keys\":[]}").build();
        }
    }

    @Path("/mock-oidc/{tenant}/introspect")
    public static class MockIntrospectionEndpoint {

        @POST
        @Produces(MediaType.APPLICATION_JSON)
        public String introspect() {
            return "{\"active\":true,\"sub\":\"test-user\",\"username\":\"test-user\"}";
        }
    }

    private static boolean isAnonymous(String tenant, String authorization) {
        return !switch (tenant) {
            case "basic" -> isBasicAuth(authorization);
            case "bearer" -> isBearerAuth(authorization, BEARER_TOKEN);
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

    private static boolean isBearerAuth(String authorization, String expectedToken) {
        return authorization != null && authorization.equals("Bearer " + expectedToken);
    }

    @ApplicationScoped
    public static class IncompatibleMethodTenantConfigResolver implements TenantConfigResolver {

        @Override
        public Uni<OidcTenantConfig> resolve(RoutingContext routingContext,
                OidcRequestContext<OidcTenantConfig> requestContext) {
            if (routingContext.normalizedPath().startsWith("/post-method/")) {
                return Uni.createFrom().item(OidcTenantConfig.builder()
                        .tenantId("post-method")
                        .authServerUrl("http://localhost:8081/mock-oidc/basic")
                        .clientId(CLIENT_ID)
                        .credentials()
                        .clientSecret(CLIENT_SECRET, OidcClientCommonConfig.Credentials.Secret.Method.POST)
                        .forAllEndpoints()
                        .end()
                        .build());
            }
            return Uni.createFrom().nullItem();
        }
    }
}
