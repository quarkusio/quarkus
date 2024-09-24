package io.quarkus.it.keycloak;

import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import io.smallrye.jwt.build.Jwt;

@TestProfile(StaticTenantIssuerResolverTest.IssuerResolverProfile.class)
@QuarkusTest
public class StaticTenantIssuerResolverTest {

    @Test
    public void testOidcServerUnavailableOnAppStartup() {
        WiremockTestResource server = new WiremockTestResource("https://correct-issuer.edu", 8185);
        server.start();
        try {
            // 500 because default tenant has unavailable OIDC server (otherwise it assumes our issuer)
            requestAdminRoles("https://wrong-issuer.edu").statusCode(500);

            requestAdminRoles("https://correct-issuer.edu").statusCode(200)
                    .body(Matchers.is("static.tenant.id=bearer-issuer-resolver"));
        } finally {
            server.stop();
        }
    }

    private static ValidatableResponse requestAdminRoles(String issuer) {
        return RestAssured.given().auth().oauth2(getAdminTokenWithRole(issuer))
                .when().get("/api/admin/bearer-issuer-resolver/issuer").then();
    }

    public static class IssuerResolverProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "issuer-based-resolver";
        }
    }

    private static String getAdminTokenWithRole(String issuer) {
        return Jwt.preferredUserName("alice")
                .groups(Set.of("admin"))
                .issuer(issuer)
                .audience(issuer)
                .jws()
                .keyId("1")
                .sign("privateKey.jwk");
    }
}
