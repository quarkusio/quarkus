package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class CodeFlowApplicationLoginPageAbsoluteUrlTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addAsResource(new StringAsset("""
                    quarkus.keycloak.devservices.enabled=false
                    quarkus.oidc.auth-server-url=http://localhost:8180/realms/quarkus
                    quarkus.oidc.client-id=quarkus-web-app
                    quarkus.oidc.application-type=web-app
                    quarkus.oidc.authentication.application-login-path=http://localhost:8081/login
                    """), "application.properties"))
            .assertException(t -> {
                Throwable e = t;
                while (e != null && !(e instanceof ConfigurationException)) {
                    e = e.getCause();
                }
                assertNotNull(e);
                assertTrue(e.getMessage().contains(
                        "'quarkus.oidc.authentication.application-login-path' must be a relative path"), e.getMessage());
            });

    @Test
    public void test() {
        Assertions.fail();
    }
}
