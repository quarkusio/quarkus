package io.quarkus.oidc.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;

class KeycloakCustomRealmExportDevUITest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar
                    .addAsResource("quarkus-realm.json")
                    .addAsResource(new StringAsset(
                            """
                                    quarkus.oidc.enabled=false
                                    quarkus.keycloak.devservices.realm-path=quarkus-realm.json
                                    """),
                            "application.properties"));

    KeycloakCustomRealmExportDevUITest() {
        super("quarkus-devservices-keycloak");
    }

    @Test
    void exportCustomRealmContainsExpectedEntities() throws Exception {
        JsonNode result = super.executeJsonRPCMethod("exportRealm");
        assertNotNull(result, "Export should return a result");
        assertTrue(result.has("success"), "Result should have 'success' field");
        assertTrue(result.get("success").asBoolean(), "Export should succeed: " + result);
        String realmJson = result.get("realmJson").asText();
        assertNotNull(realmJson, "Exported JSON should not be null");
        assertTrue(realmJson.contains("jdoe"), "Exported JSON should contain user jdoe");
        assertTrue(realmJson.contains("service-account-backend-service"),
                "Exported JSON should contain user service-account-backend-service");
        assertTrue(realmJson.contains("alice"), "Exported JSON should contain user alice");
        assertTrue(realmJson.contains("admin"), "Exported JSON should contain user admin");
        assertTrue(realmJson.contains("backend-service"), "Exported JSON should contain client backend-service");
    }
}
