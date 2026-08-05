package io.quarkus.restclient.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.restclient.config.RestClientsConfig.RestClientConfig;
import io.quarkus.test.QuarkusExtensionTest;

class RestClientRandomPortTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
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
        assertThat(echoClientConfig.url()).hasValue("http://localhost:0");
        assertThat(echoClientConfig.urlReload()).hasValueSatisfying(val -> assertThat(val).isNotEqualTo("http://localhost:0"));
    }

    @Test
    public void shouldRespond() {
        assertEquals("Hello", echoClient.echo("Hello"));
    }
}
