package io.quarkus.it.keycloak;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import io.smallrye.jwt.build.Jwt;

@TestProfile(StaticTenantIssuerResolverOnceTest.IssuerResolverOnceProfile.class)
@QuarkusTest
public class StaticTenantIssuerResolverOnceTest {

    private static final long RETRY_INTERVAL_MILLIS = 3000;

    @Test
    public void testTenantStaysUnresolvedAfterFailedFirstAttempt() throws InterruptedException {
        // provider is down; this request consumes the single always-allowed initialization attempt
        requestAdminRoles().statusCode(500);

        WiremockTestResource server = new WiremockTestResource("https://correct-issuer.edu", 8185);
        server.start();
        try {
            // default retry interval is 0S, so the failed attempt is never retried, even though the provider is now healthy
            requestAdminRoles().statusCode(500);

            Thread.sleep(RETRY_INTERVAL_MILLIS);

            requestAdminRoles().statusCode(500);
        } finally {
            server.stop();
        }
    }

    private static ValidatableResponse requestAdminRoles() {
        var token = Jwt.preferredUserName("alice")
                .groups(Set.of("admin"))
                .issuer("https://correct-issuer.edu")
                .audience("https://correct-issuer.edu")
                .jws()
                .keyId("1")
                .sign("privateKey.jwk");
        return RestAssured.given().auth().oauth2(token)
                .when().get("/api/admin/bearer-issuer-resolver/issuer").then();
    }

    public static class IssuerResolverOnceProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.oidc.resolve-tenants-with-issuer", "true",
                    "quarkus.oidc.bearer-issuer-resolver-a.auth-server-url", "http://localhost:8185/auth/realms/quarkus2",
                    "quarkus.oidc.bearer-issuer-resolver-a.discovery-path", ".well-known/oauth-authorization-server",
                    "quarkus.oidc.bearer-issuer-resolver-a.client-id", "quarkus-app",
                    "quarkus.oidc.bearer-issuer-resolver-a.credentials.secret", "secret",
                    "quarkus.oidc.bearer-issuer-resolver-a.token.audience", "https://correct-issuer.edu");
        }
    }
}
