package io.quarkus.oidc.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A refresh only starts once the remaining lifespan has dropped below the skew, so a minimum which is not
 * less than the skew could never be satisfied and would silently disable the reuse of the token being refreshed.
 */
public class OidcClientMinRemainingAccessTokenLifespanNotLessThanSkewTestCase {

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
                                    + "quarkus.oidc-client.min-remaining-access-token-lifespan=10S\n"),
                            "application.properties"))
            .assertException(t -> {
                ConfigurationException te = configurationException(t);
                assertNotNull(te, "Expected ConfigurationException, but got: " + t);
                assertTrue(
                        te.getMessage().contains(
                                "'quarkus.oidc-client.min-remaining-access-token-lifespan' must be less than"
                                        + " 'quarkus.oidc-client.refresh-token-time-skew'"),
                        te.getMessage());
            });

    static ConfigurationException configurationException(Throwable t) {
        Throwable e = t;
        while (e != null) {
            if (e instanceof ConfigurationException) {
                return (ConfigurationException) e;
            }
            e = e.getCause();
        }
        return null;
    }

    @Test
    public void test() {
        Assertions.fail();
    }

}
