package io.quarkus.keycloak.pep.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class DefaultTenantNonProxyHostsValidationFailureTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.keycloak.policy-enforcer.enabled", "true")
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideRuntimeConfigKey("quarkus.oidc.auth-server-url", "http://localhost:8180/realms/quarkus")
            .overrideRuntimeConfigKey("quarkus.oidc.client-id", "quarkus-app")
            .overrideRuntimeConfigKey("quarkus.oidc.proxy.proxy-configuration-name", "my-proxy")
            .overrideRuntimeConfigKey("quarkus.proxy.my-proxy.host", "localhost")
            .overrideRuntimeConfigKey("quarkus.proxy.my-proxy.port", "3128")
            .overrideRuntimeConfigKey("quarkus.proxy.my-proxy.non-proxy-hosts", "localhost")
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("quarkus.proxy.my-proxy.non-proxy-hosts"));

    @Test
    public void test() {
        fail("Application startup should fail");
    }
}
