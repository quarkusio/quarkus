package io.quarkus.rest.client.reactive.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.SetSystemProperty;

import io.quarkus.test.QuarkusUnitTest;

public class SystemPropertyProxyWithAuthTest extends ProxyTestBase {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(
                    jar -> jar.addClasses(Client1.class, Client2.class, Client3.class, ViaHeaderReturningResource.class))
            .withConfigurationResource("system-props-proxy-test-application.properties");

    @RestClient
    Client1 client1;
    @RestClient
    Client2 client2;

    /*
     * - client1 should use JVM settings, set with -Dhttp.proxyHost, etc (8183, with auth)
     * - CDI managed client2 should use client specific settings as configured in the properties (8181)
     * - client created with builder should use system settings by default (8183, with auth)
     */
    @Test
    @SetSystemProperty(key = "http.proxyHost", value = "localhost")
    @SetSystemProperty(key = "http.proxyPort", value = "8183")
    @SetSystemProperty(key = "http.proxyUser", value = "admin")
    @SetSystemProperty(key = "http.proxyPassword", value = "r00t")
    @SetSystemProperty(key = "http.nonProxyHosts", value = "example.com")
    void shouldProxyWithCredentialsFromProperties() {
        assertThat(client1.get().readEntity(String.class)).isEqualTo(AUTHENTICATED_PROXY);
        assertThat(client2.get().readEntity(String.class)).isEqualTo(PROXY_8181);

        Response response2 = RestClientBuilder.newBuilder().baseUri(appUri).build(Client2.class).get();
        assertThat(response2.readEntity(String.class)).isEqualTo(AUTHENTICATED_PROXY);
    }

}
