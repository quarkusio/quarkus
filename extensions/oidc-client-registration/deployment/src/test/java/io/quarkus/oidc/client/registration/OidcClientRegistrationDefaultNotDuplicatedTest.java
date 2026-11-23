package io.quarkus.oidc.client.registration;

import static io.quarkus.oidc.client.registration.runtime.OidcClientRegistrationsConfig.DEFAULT_CLIENT_REGISTRATION_KEY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

class OidcClientRegistrationDefaultNotDuplicatedTest {

    private static final String NAMED_CLIENT_REGISTRATION_KEY = "named";

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withConfiguration("""
                    quarkus.keycloak.devservices.enabled=false
                    quarkus.oidc-client-registration.registration-path=http://localhost:8081/default/register
                    quarkus.oidc-client-registration.%s.registration-path=http://localhost:8081/named/register
                    """.formatted(NAMED_CLIENT_REGISTRATION_KEY));

    @Inject
    OidcClientRegistrations clientRegistrations;

    @Test
    void defaultClientRegistrationIsNotDuplicatedAsNamedRegistration() {
        // the default registration is configured, so it is created and exposed as the default
        assertNotNull(clientRegistrations.getClientRegistration());

        Map<String, OidcClientRegistration> namedRegistrations = clientRegistrations.getClientRegistrations();
        // named registrations are still created from their configuration
        assertTrue(namedRegistrations.containsKey(NAMED_CLIENT_REGISTRATION_KEY),
                () -> "Expected a named registration under key '" + NAMED_CLIENT_REGISTRATION_KEY + "', but found: "
                        + namedRegistrations.keySet());
        // but the default registration must not be created a second time under the '<default>' key
        assertFalse(namedRegistrations.containsKey(DEFAULT_CLIENT_REGISTRATION_KEY),
                () -> "Default client registration must not be duplicated as a named registration, but found: "
                        + namedRegistrations.keySet());
    }
}
