package io.quarkus.restclient.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.restclient.config.RestClientsConfig.RestClientConfig;
import io.quarkus.test.QuarkusUnitTest;

class RestClientRandomPortTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(EchoResource.class, EchoClient.class))
            .overrideRuntimeConfigKey("quarkus.http.port", "0")
            .overrideRuntimeConfigKey("quarkus.http.test-port", "0")
            .overrideRuntimeConfigKey("quarkus.rest-client.EchoClient.url", "http://localhost:${quarkus.http.port}");

    @Inject
    RestClientsConfig restClientsConfig;
    @RestClient
    EchoClient echoClient;

    @Test
    void config() {
        RestClientConfig echoClientConfig = restClientsConfig.getClient(EchoClient.class);
        assertTrue(echoClientConfig.url().isPresent());
        assertEquals("http://localhost:0", echoClientConfig.url().get());
        assertNotEquals("http://localhost:0", echoClientConfig.urlReload());
    }

    @Test
    public void shouldRespond() {
        assertEquals("Hello", echoClient.echo("Hello"));
    }
}
