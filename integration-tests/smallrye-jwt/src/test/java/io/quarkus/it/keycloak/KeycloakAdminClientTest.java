package io.quarkus.it.keycloak;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@TestProfile(KeycloakAdminClientTest.EnableKeycloakDevServicesProfile.class)
@EnabledIfSystemProperty(named = "test-containers", matches = "true", disabledReason = "Requires Keycloak")
@QuarkusTest
public class KeycloakAdminClientTest {

    @Test
    void putEmptyRealmClientProfiles() {
        when().put("/keycloak-admin-client/empty-realm-client-profiles")
                .then()
                .statusCode(200)
                .body(is("quarkus:0"));
    }

    public static final class EnableKeycloakDevServicesProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.keycloak.devservices.enabled", "true");
        }
    }
}
