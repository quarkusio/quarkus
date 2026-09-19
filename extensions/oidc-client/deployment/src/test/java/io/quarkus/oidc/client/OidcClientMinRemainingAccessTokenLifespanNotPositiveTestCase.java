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
 * A zero minimum remaining lifespan would allow an access token with nothing left to be reused, which is what
 * this feature exists to prevent, so it is rejected rather than treated as "no minimum".
 */
public class OidcClientMinRemainingAccessTokenLifespanNotPositiveTestCase {

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
                                    + "quarkus.oidc-client.min-remaining-access-token-lifespan=0S\n"),
                            "application.properties"))
            .assertException(t -> {
                ConfigurationException te = configurationException(t);
                assertNotNull(te, "Expected ConfigurationException, but got: " + t);
                assertTrue(
                        te.getMessage().contains(
                                "'quarkus.oidc-client.min-remaining-access-token-lifespan' must be greater than zero"),
                        te.getMessage());
            });

    @Test
    public void test() {
        Assertions.fail();
    }

}
