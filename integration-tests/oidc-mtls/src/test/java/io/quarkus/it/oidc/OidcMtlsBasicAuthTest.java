package io.quarkus.it.oidc;

import static io.quarkus.it.oidc.OidcMtlsTest.createWebClientOptions;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.quarkus.test.keycloak.client.KeycloakTestClient.Tls;
import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

@TestProfile(OidcMtlsBasicAuthTest.LaxInclusiveModeProfile.class)
@QuarkusTest
@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
public class OidcMtlsBasicAuthTest {

    KeycloakTestClient client = new KeycloakTestClient(
            new Tls("target/certificates/oidc-client-keystore.p12",
                    "target/certificates/oidc-client-truststore.p12"));

    @TestHTTPResource(tls = true)
    URL url;

    @Inject
    Vertx vertx;

    @Test
    public void testMtlsJwtLax() throws Exception {
        // verifies that in LAX mode, it is permitted that not all the mechanisms need to create the identity
        WebClientOptions options = createWebClientOptions(url);
        WebClient webClient = WebClient.create(new io.vertx.mutiny.core.Vertx(vertx), options);

        try {
            // HTTP 200
            HttpResponse<io.vertx.mutiny.core.buffer.Buffer> resp = webClient.get("/multiple-auth-mechanisms/mtls-jwt-lax")
                    .putHeader("Authorization",
                            OidcConstants.BEARER_SCHEME + " " + getAccessToken("backend-service", null, "alice"))
                    .send().await()
                    .indefinitely();
            assertEquals(200, resp.statusCode());

            // HTTP 401, invalid token
            resp = webClient.get("/multiple-auth-mechanisms/mtls-jwt-lax")
                    .putHeader("Authorization", OidcConstants.BEARER_SCHEME + " " + "123")
                    .send().await()
                    .indefinitely();
            assertEquals(401, resp.statusCode());

            // HTTP 200, no token and inclusive authentication in the lax mode, therefore 200
            resp = webClient.get("/multiple-auth-mechanisms/mtls-jwt-lax").send().await().indefinitely();
            assertEquals(200, resp.statusCode());
        } finally {
            webClient.close();
        }
    }

    @Test
    public void testMtlsJwt() throws Exception {
        // verifies that in LAX mode, it is permitted that not all the mechanisms need to create the identity
        WebClientOptions options = createWebClientOptions(url);
        WebClient webClient = WebClient.create(new io.vertx.mutiny.core.Vertx(vertx), options);

        try {
            // HTTP 200
            HttpResponse<io.vertx.mutiny.core.buffer.Buffer> resp = webClient.get("/multiple-auth-mechanisms/mtls-jwt")
                    .putHeader("Authorization",
                            OidcConstants.BEARER_SCHEME + " " + getAccessToken("backend-service", null, "alice"))
                    .send().await()
                    .indefinitely();
            assertEquals(200, resp.statusCode());

            // HTTP 401, invalid token
            resp = webClient.get("/multiple-auth-mechanisms/mtls-jwt")
                    .putHeader("Authorization", OidcConstants.BEARER_SCHEME + " " + "123")
                    .send().await()
                    .indefinitely();
            assertEquals(401, resp.statusCode());

            // HTTP 403, no token and inclusive authentication in the lax mode,
            // but permission checker requires both JWT and mTLS
            resp = webClient.get("/multiple-auth-mechanisms/mtls-jwt").send().await().indefinitely();
            assertEquals(403, resp.statusCode());
        } finally {
            webClient.close();
        }
    }

    @Test
    public void testMtlsBasic() throws Exception {
        WebClientOptions options = createWebClientOptions(url);
        WebClient webClient = WebClient.create(new io.vertx.mutiny.core.Vertx(vertx), options);

        try {
            // HTTP 403, basic & mTLS are expected and basic is missing
            HttpResponse<io.vertx.mutiny.core.buffer.Buffer> resp = webClient.get("/multiple-auth-mechanisms/mtls-basic")
                    .putHeader("Authorization",
                            OidcConstants.BEARER_SCHEME + " " + getAccessToken("backend-service", null, "alice"))
                    .send().await()
                    .indefinitely();
            assertEquals(403, resp.statusCode());

            // HTTP 200, basic & mTLS are expected
            resp = webClient.get("/multiple-auth-mechanisms/mtls-basic")
                    .putHeader("Authorization",
                            new UsernamePasswordCredentials("Gaston", "Gaston").applyHttpChallenge(null).toHttpAuthorization())
                    .send().await()
                    .indefinitely();
            assertEquals(200, resp.statusCode());

            // HTTP 403, only basic but mTLS & basic are required
            RestAssured
                    .given()
                    .auth().preemptive().basic("Gaston", "Gaston")
                    .get("/multiple-auth-mechanisms/mtls-basic")
                    .then()
                    .statusCode(403);
        } finally {
            webClient.close();
        }
    }

    private String getAccessToken(String clientName, String clientSecret, String userName) {
        return client.getAccessToken(userName, userName, clientName, clientSecret);
    }

    public static class LaxInclusiveModeProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.http.auth.inclusive-mode", "lax",
                    "quarkus.http.ssl.client-auth", "request",
                    "quarkus.http.insecure-requests", "enabled",
                    "quarkus.http.auth.basic", "true",
                    "quarkus.oidc.mtls-jwt.tenant-paths", "/*/mtls-jwt-lax,/*/mtls-jwt,/*/mtls-basic");
        }
    }

}
