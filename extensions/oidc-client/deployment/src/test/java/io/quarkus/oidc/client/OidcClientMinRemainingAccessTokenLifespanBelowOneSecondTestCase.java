package io.quarkus.oidc.client;

import static io.quarkus.oidc.client.OidcClientMinRemainingAccessTokenLifespanNotLessThanSkewTestCase.configurationException;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The access token expiry is only tracked in whole seconds, so a minimum remaining lifespan below a second
 * would be truncated to zero and silently reuse every token which has not expired yet. It is rejected instead,
 * so that the minimum which is applied is always the one which was configured.
 */
public class OidcClientMinRemainingAccessTokenLifespanBelowOneSecondTestCase {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(
                            "# Disable Dev Services, Keycloak is started by a Maven plugin\n"
                                    + "quarkus.keycloak.devservices.enabled=false\n"
                                    + "quarkus.oidc-client.token-path=http://localhost:8180/oidc/tokens\n"
                                    + "quarkus.oidc-client.client-id=quarkus\n"
                                    + "quarkus.oidc-client.credentials.secret=secret\n"
                                    + "quarkus.oidc-client.refresh-token-time-skew=10S\n"
                                    + "quarkus.oidc-client.min-remaining-access-token-lifespan=500ms\n"),
                            "application.properties"))
            .assertException(t -> {
                ConfigurationException te = configurationException(t);
                assertNotNull(te, "Expected ConfigurationException, but got: " + t);
                assertTrue(
                        te.getMessage().contains(
                                "'quarkus.oidc-client.min-remaining-access-token-lifespan' must be greater than 0 seconds"),
                        te.getMessage());
            });

    @Test
    public void test() {
        Assertions.fail();
    }

}
