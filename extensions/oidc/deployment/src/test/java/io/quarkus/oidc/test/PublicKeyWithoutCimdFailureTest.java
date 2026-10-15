package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class PublicKeyWithoutCimdFailureTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(
                            "quarkus.oidc.auth-server-url=http://localhost/oidc\n"
                                    + "quarkus.oidc.discovery-enabled=false\n"
                                    + "quarkus.oidc.application-type=web-app\n"
                                    + "quarkus.oidc.client-id=my-client\n"
                                    + "quarkus.oidc.credentials.jwt.key=dummy-private-key\n"
                                    + "quarkus.oidc.credentials.jwt.public-key=dummy-public-key\n"),
                            "application.properties"))
            .assertException(t -> {
                // the exception is loaded by the Quarkus runtime class loader, so it can only be matched by name
                Throwable e = t;
                while (e != null && !ConfigurationException.class.getName().equals(e.getClass().getName())) {
                    e = e.getCause();
                }
                assertNotNull(e);
                assertTrue(
                        e.getMessage().contains("A JWT public key is configured")
                                && e.getMessage().contains("Client ID Metadata Document is not enabled"),
                        e.getMessage());
            });

    @Test
    public void test() {
        Assertions.fail();
    }

}
