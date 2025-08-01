package io.quarkus.oidc.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class OidcClientTooManyJwtCredentialKeyPropsTestCase {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(
                            "# Disable Dev Services, Keycloak is started by a Maven plugin\n"
                                    + "quarkus.keycloak.devservices.enabled=false\n"
                                    + "quarkus.oidc-client.token-path=http://localhost:8180/oidc/tokens\n"
                                    + "quarkus.oidc-client.client-id=quarkus\n"
                                    + "quarkus.oidc-client.credentials.jwt.secret=secret\n"
                                    + "quarkus.oidc-client.credentials.jwt.key=base64encPrivateKey\n"
                                    + "quarkus.oidc-client.grant.type=jwt"),
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
                assertEquals(
                        "Only a single OIDC JWT credential key property can be configured, but you have configured:"
                                + " quarkus.oidc-client.credentials.jwt.key,quarkus.oidc-client.credentials.jwt.secret",
                        te.getMessage(),
                        "Too many JWT credential key properties are configured");
            });

    @Test
    public void test() {
        Assertions.fail();
    }

}
