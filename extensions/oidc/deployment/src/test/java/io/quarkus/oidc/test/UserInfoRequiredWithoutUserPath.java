package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class UserInfoRequiredWithoutUserPath {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(
                    (jar) -> jar.addAsResource(new StringAsset("quarkus.oidc.authentication.id-token-required=false\n"
                            + "quarkus.oidc.authorization-path=authorize\n" + "quarkus.oidc.token-path=token\n"
                            + "quarkus.oidc.application-type=web-app\n"
                            + "quarkus.oidc.authentication.verify-access-token=false\n"
                            + "quarkus.oidc.discovery-enabled=false\n"), "application.properties"))
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
                                .contains("UserInfo is required but 'quarkus.oidc.user-info-path' is not configured."),
                        te.getMessage());
            });

    @Test
    public void test() {
        Assertions.fail();
    }

}
