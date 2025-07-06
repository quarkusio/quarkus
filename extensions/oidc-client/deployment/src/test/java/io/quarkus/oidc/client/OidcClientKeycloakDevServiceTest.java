package io.quarkus.oidc.client;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Test Keycloak Dev Service is started when OIDC extension is disabled (or not present, though indirectly).
 * OIDC client auth server URL and client id and secret must be automatically configured for this test to pass.
 * This test uses Dev Services for Keycloak.
 */
public class OidcClientKeycloakDevServiceTest extends AbstractOidcClientDevServiceTest {

    @RegisterExtension
    static final QuarkusUnitTest test = createQuarkusUnitTest("oidc-client-dev-service-test.properties");

}
