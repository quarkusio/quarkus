package io.quarkus.oidc.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.vertx.core.json.JsonObject;

/**
 * Test Keycloak Dev Service is started when OIDC extension is disabled (or not present, though indirectly).
 * OIDC client auth server URL and client id and secret must be automatically configured for this test to pass.
 * This test uses Dev Services for Keycloak.
 */
public class OidcClientKeycloakDevServiceTest extends AbstractOidcClientDevServiceTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = createQuarkusExtensionTest("oidc-client-dev-service-test.properties");

    @Test
    public void testAccessTokenLifespan() {
        String accessToken = RestAssured.given().get("/client1/token").body().asString();
        JsonObject accessJson = OidcCommonUtils.decodeJwtContent(accessToken);
        long accessExp = accessJson.getLong("exp");
        long accessIat = accessJson.getLong("iat");
        assertEquals(150L, accessExp - accessIat, 5L);
    }

    @Test
    public void testRefreshTokenLifespan() {
        String refreshToken = RestAssured.given().get("/client1/refreshToken").body().asString();
        JsonObject refreshJson = OidcCommonUtils.decodeJwtContent(refreshToken);
        long refreshExp = refreshJson.getLong("exp");
        long refreshIat = refreshJson.getLong("iat");
        assertEquals(300L, refreshExp - refreshIat, 5L);
    }
}
