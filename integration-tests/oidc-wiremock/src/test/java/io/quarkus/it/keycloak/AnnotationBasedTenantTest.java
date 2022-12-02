package io.quarkus.it.keycloak;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;

@QuarkusTest
@TestProfile(AnnotationBasedTenantTest.NoProactiveAuthTestProfile.class)
@QuarkusTestResource(OidcWiremockTestResource.class)
public class AnnotationBasedTenantTest {
    public static class NoProactiveAuthTestProfile implements QuarkusTestProfile {
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.http.auth.proactive", "false");
        }
    }

    @Test
    public void test() {
        String token = OidcWiremockTestResource.getAccessToken("alice", new HashSet<>(Arrays.asList("user", "admin")));

        // Server is starting now
        WiremockTestResource server = new WiremockTestResource();
        server.start();
        try {
            RestAssured.given().auth().oauth2(token)
                    .when().get("/api/tenant-echo")
                    .then().statusCode(200)
                    .body(StringContains.containsString("tenant-id=hr"))
                    .body(StringContains.containsString("static.tenant.id=hr"));
        } finally {
            server.stop();
        }
    }
}
