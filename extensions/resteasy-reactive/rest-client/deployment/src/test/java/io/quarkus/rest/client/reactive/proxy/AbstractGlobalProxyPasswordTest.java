package io.quarkus.rest.client.reactive.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import io.quarkus.test.QuarkusUnitTest;

public abstract class AbstractGlobalProxyPasswordTest extends ProxyTestBase {
    protected static QuarkusUnitTest config(String applicationProperties) {
        return new QuarkusUnitTest()
                .withApplicationRoot(
                        jar -> jar.addClasses(Client1.class, ViaHeaderReturningResource.class))
                .withConfigurationResource(applicationProperties);
    }

    @RestClient
    Client1 client1;

    @Test
    void shouldProxyCDIWithPerClientSettings() {
        assertThat(client1.get().readEntity(String.class)).isEqualTo(AUTHENTICATED_PROXY);
    }
}
