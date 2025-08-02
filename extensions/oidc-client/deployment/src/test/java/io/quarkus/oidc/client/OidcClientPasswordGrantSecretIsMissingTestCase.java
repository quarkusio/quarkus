package io.quarkus.oidc.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class OidcClientPasswordGrantSecretIsMissingTestCase {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(
                            "# Disable Dev Services, Keycloak is started by a Maven plugin\n"
                                    + "quarkus.keycloak.devservices.enabled=false\n"
                                    + "quarkus.oidc-client.token-path=http://localhost:8180/oidc/tokens\n"
                                    + "quarkus.oidc-client.client-id=quarkus\n"
                                    + "quarkus.oidc-client.credentials.secret=secret\n"
                                    + "quarkus.oidc-client.grant.type=password\n"
                                    + "quarkus.oidc-client.grant-options.password.user=alice\n"),
                            "application.properties"))
            .assertException(t -> {
                Throwable e = t;
                ConfigurationException te = null;
                while (e != null) {
                    if (e instanceof ConfigurationException) {
                        te = (ConfigurationException) e;
                        break;
                    }
                    e = e.getCause();
                }
                assertNotNull(te, "Expected ConfigurationException, but got: " + t);
                assertTrue(
                        te.getMessage()
                                .contains("Username and password must be set when a password grant is used"),
                        te.getMessage());
            });

    @Test
    public void test() {
        Assertions.fail();
    }

}
