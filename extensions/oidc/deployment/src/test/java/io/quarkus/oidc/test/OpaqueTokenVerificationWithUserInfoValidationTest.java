package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class OpaqueTokenVerificationWithUserInfoValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(
                            "# Disable Dev Services, we use a test resource manager\n" +
                                    "quarkus.keycloak.devservices.enabled=false\n" +
                                    "quarkus.oidc.token.verify-access-token-with-user-info=true\n"
                                    + "quarkus.oidc.authentication.user-info-required=false\n"),
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
                assertNotNull(te);
                // assert UserInfo is required
                assertTrue(
                        te.getMessage()
                                .contains(
                                        "UserInfo is not required but 'quarkus.oidc.token.verify-access-token-with-user-info' is enabled"),
                        te.getMessage());
            });

    @Test
    public void test() {
        Assertions.fail();
    }

}
