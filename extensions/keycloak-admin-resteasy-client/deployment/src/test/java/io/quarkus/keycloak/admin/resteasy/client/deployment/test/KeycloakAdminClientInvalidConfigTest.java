package io.quarkus.keycloak.admin.resteasy.client.deployment.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class KeycloakAdminClientInvalidConfigTest {

    @RegisterExtension
    final static QuarkusExtensionTest app = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addAsResource(new StringAsset("""
                            quarkus.keycloak.devservices.enabled=false
                            quarkus.keycloak.admin-client.server-url=http://localhost:1
                            quarkus.keycloak.admin-client.grant-type=CLIENT_CREDENTIALS
                            """), "application.properties"))
            .assertException(t -> {
                Throwable cause = t;
                while (cause != null
                        && !String.valueOf(cause.getMessage())
                                .contains("grant type 'client_credentials' requires client secret")) {
                    cause = cause.getCause();
                }
                assertNotNull(cause, "Unexpected startup failure: " + t);
            });

    @Test
    public void test() {
        fail("Startup should have failed");
    }
}
