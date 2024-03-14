package io.quarkus.rest.client.reactive.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class GlobalProxyPasswordTest extends ProxyTestBase {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(
                    jar -> jar.addClasses(Client1.class, ViaHeaderReturningResource.class))
            .withConfigurationResource("global-proxy-password-test-application.properties");

    @RestClient
    Client1 client1;

    @Test
    void shouldProxyCDIWithPerClientSettings() {
        assertThat(client1.get().readEntity(String.class)).isEqualTo(AUTHENTICATED_PROXY);
    }
}
