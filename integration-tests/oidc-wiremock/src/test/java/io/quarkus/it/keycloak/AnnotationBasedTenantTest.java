package io.quarkus.it.keycloak;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;
import io.smallrye.jwt.build.Jwt;

@QuarkusTest
@TestProfile(AnnotationBasedTenantTest.NoProactiveAuthTestProfile.class)
@QuarkusTestResource(OidcWiremockTestResource.class)
public class AnnotationBasedTenantTest {
    public static class NoProactiveAuthTestProfile implements QuarkusTestProfile {
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.http.auth.proactive", "false",
                    "quarkus.oidc.hr.auth-server-url", "http://localhost:8180/auth/realms/quarkus2/",
                    "quarkus.oidc.hr.client-id", "quarkus-app",
                    "quarkus.oidc.hr.credentials.secret", "secret",
                    "quarkus.oidc.hr.token.audience", "http://hr.service");
        }
    }

    @Test
    public void testClassLevelAnnotation() {
        // Server is starting now
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            // Audience is wrong
            String token = OidcWiremockTestResource.getAccessToken("alice", new HashSet<>(Arrays.asList("user", "admin")));
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo")
                    .then().statusCode(401);

            token = Jwt.preferredUserName("alice")
                    .audience("http://hr.service")
                    .jws()
                    .keyId("1")
                    .sign("privateKey.jwk");

            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo")
                    .then().statusCode(200)
                    .body(Matchers.equalTo(("tenant-id=hr, static.tenant.id=hr, name=alice")));

        } finally {
            server.stop();
        }
    }

    @Test
    public void testMethodLevelAnnotation() {
        // Server is starting now
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            // ANNOTATED ENDPOINT
            // Audience is wrong
            String token = OidcWiremockTestResource.getAccessToken("alice", new HashSet<>(Arrays.asList("user", "admin")));
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr")
                    .then().statusCode(401);

            token = Jwt.preferredUserName("alice")
                    .audience("http://hr.service")
                    .jws()
                    .keyId("1")
                    .sign("privateKey.jwk");

            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/hr")
                    .then().statusCode(200)
                    .body(Matchers.equalTo(("tenant-id=hr, static.tenant.id=hr, name=alice")));

            // UNANNOTATED ENDPOINT
            token = OidcWiremockTestResource.getAccessToken("alice", new HashSet<>(Arrays.asList("user", "admin")));

            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo2/default")
                    .then().statusCode(200)
                    .body(Matchers.equalTo(("tenant-id=null, static.tenant.id=null, name=alice")));
        } finally {
            server.stop();
        }
    }
}
