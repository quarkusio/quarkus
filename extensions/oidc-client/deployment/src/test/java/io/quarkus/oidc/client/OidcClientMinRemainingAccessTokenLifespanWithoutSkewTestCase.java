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
 * Without a refresh token time skew no refresh starts before the access token has expired, so there is never a
 * valid token to reuse and a minimum remaining lifespan on its own would have no effect.
 */
public class OidcClientMinRemainingAccessTokenLifespanWithoutSkewTestCase {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(
                            "# Disable Dev Services, Keycloak is started by a Maven plugin\n"
                                    + "quarkus.keycloak.devservices.enabled=false\n"
                                    + "quarkus.oidc-client.token-path=http://localhost:8180/oidc/tokens\n"
                                    + "quarkus.oidc-client.client-id=quarkus\n"
                                    + "quarkus.oidc-client.credentials.secret=secret\n"
                                    + "quarkus.oidc-client.min-remaining-access-token-lifespan=5S\n"),
                            "application.properties"))
            .assertException(t -> {
                ConfigurationException te = configurationException(t);
                assertNotNull(te, "Expected ConfigurationException, but got: " + t);
                assertTrue(
                        te.getMessage().contains(
                                "'quarkus.oidc-client.min-remaining-access-token-lifespan' requires"
                                        + " 'quarkus.oidc-client.refresh-token-time-skew' to be configured"),
                        te.getMessage());
            });

    @Test
    public void test() {
        Assertions.fail();
    }

}
