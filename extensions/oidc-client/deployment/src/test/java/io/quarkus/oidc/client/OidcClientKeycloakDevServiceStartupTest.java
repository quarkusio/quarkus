package io.quarkus.oidc.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Test Keycloak Dev Service is not started when known social provider is configured
 * in Quarkus OIDC extension.
 */
public class OidcClientKeycloakDevServiceStartupTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addAsResource(new StringAsset("""
                            # Disable Dev Services, Keycloak is started by a Maven plugin
                            quarkus.keycloak.devservices.enabled=false

                            quarkus.oidc.provider=slack
                            quarkus.oidc.client-id=irrelevant-client-id
                            """), "application.properties"))
            .setLogRecordPredicate(logRecord -> logRecord != null && logRecord.getMessage() != null
                    && logRecord.getMessage().contains("Dev Services for Keycloak started"))
            .assertLogRecords(logRecords -> assertTrue(logRecords.isEmpty()));

    @Test
    public void testDevServiceNotStarted() {
        // needs to be here so that log asserter runs after all tests
    }

}
