package io.quarkus.vertx.http.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class HttpPolicyConfigurationWithSemicolonTestCase {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(
                            "quarkus.http.auth.permission.matrix.paths=/a;\n"
                                    + "quarkus.http.auth.permission.matrix.policy=authenticated"),
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
                assertTrue(
                        te.getMessage()
                                .contains("HttpSecurityPolicy '/a;' path contains a semicolon ';' character."),
                        te.getMessage());
            });

    @Test
    public void test() {
        Assertions.fail();
    }

}
