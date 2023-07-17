package io.quarkus.rest.client.reactive.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class GlobalNonProxyTest extends ProxyTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(
                    jar -> jar.addClasses(Client1.class, Client2.class, Client3.class, Client4.class, Client5.class,
                            Client6.class, ViaHeaderReturningResource.class))
            .withConfigurationResource("global-non-proxy-test-application.properties");

    @RestClient
    Client1 client1;

    @Test
    void shouldNotApplyProxyIfNonProxyMatches() {
        assertThat(client1.get().readEntity(String.class)).isEqualTo(NO_PROXY);
    }

    @Test
    void shouldProxyBuilderWithPerClientSettings() {
        Response response = RestClientBuilder.newBuilder().baseUri(appUri)
                .build(Client1.class).get();
        assertThat(response.readEntity(String.class)).isEqualTo(NO_PROXY);
    }
}
