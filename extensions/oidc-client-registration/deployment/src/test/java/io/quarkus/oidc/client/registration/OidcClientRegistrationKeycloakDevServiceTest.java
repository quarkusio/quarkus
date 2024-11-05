package io.quarkus.oidc.client.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;

public class OidcClientRegistrationKeycloakDevServiceTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(
                            new StringAsset(
                                    """
                                            quarkus.oidc-client-registration.metadata.client-name=Default Test Client
                                            quarkus.oidc-client-registration.metadata.redirect-uri=http://localhost:8081/default/redirect
                                            quarkus.oidc-client-registration.named.metadata.client-name=Named Test Client
                                            quarkus.oidc-client-registration.named.metadata.redirect-uri=http://localhost:8081/named/redirect
                                            quarkus.oidc-client-registration.named.auth-server-url=${quarkus.oidc-client-registration.auth-server-url}
                                            """),
                            "application.properties"));

    @Inject
    TestClientRegistrations testClientRegistrations;

    @Test
    public void testDefaultRegisteredClient() {
        assertEquals("Default Test Client", testClientRegistrations.defaultClientMetadata.getClientName());
        assertEquals("http://localhost:8081/default/redirect",
                testClientRegistrations.defaultClientMetadata.getRedirectUris().get(0));
    }

    @Test
    public void testNamedRegisteredClient() {
        assertEquals("Named Test Client", testClientRegistrations.namedClientMetadata.getClientName());
        assertEquals("http://localhost:8081/named/redirect",
                testClientRegistrations.namedClientMetadata.getRedirectUris().get(0));
    }

    @Singleton
    public static final class TestClientRegistrations {

        private volatile ClientMetadata defaultClientMetadata;
        private volatile ClientMetadata namedClientMetadata;

        void prepareDefaultClientMetadata(@Observes StartupEvent event, OidcClientRegistrations clientRegistrations) {
            var clientRegistration = clientRegistrations.getClientRegistration();
            var registeredClient = clientRegistration.registeredClient().await().indefinitely();
            defaultClientMetadata = registeredClient.metadata();

            clientRegistration = clientRegistrations.getClientRegistration("named");
            registeredClient = clientRegistration.registeredClient().await().indefinitely();
            namedClientMetadata = registeredClient.metadata();
        }
    }
}
