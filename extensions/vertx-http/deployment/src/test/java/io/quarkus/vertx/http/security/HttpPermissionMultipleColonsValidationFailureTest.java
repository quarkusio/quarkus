package io.quarkus.vertx.http.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

class HttpPermissionMultipleColonsValidationFailureTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("""
                            quarkus.http.auth.policy.bad.roles-allowed=test
                            quarkus.http.auth.policy.bad.permissions.test=system:role:query1
                            quarkus.http.auth.permission.bad.paths=/test/bad
                            quarkus.http.auth.permission.bad.policy=bad
                            """), "application.properties"))
            .assertException(t -> {
                Throwable e = t;
                ConfigurationException ce = null;
                while (e != null) {
                    if (e instanceof ConfigurationException) {
                        ce = (ConfigurationException) e;
                        break;
                    }
                    e = e.getCause();
                }
                assertNotNull(ce, "Expected ConfigurationException but got: " + t);
                assertTrue(ce.getMessage().contains("Invalid permission format"),
                        "Error should mention invalid format: " + ce.getMessage());
                assertTrue(ce.getMessage().contains("system:role:query1"),
                        "Error should reference the invalid value: " + ce.getMessage());
            });

    @Test
    void test() {
        Assertions.fail("Build was expected to fail due to multiple unescaped colons in permission config");
    }
}
