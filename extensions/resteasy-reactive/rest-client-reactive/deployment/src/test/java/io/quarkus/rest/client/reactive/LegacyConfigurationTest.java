package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.configuration.EchoResource;
import io.quarkus.restclient.config.RestClientConfig;
import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.test.QuarkusUnitTest;

public class LegacyConfigurationTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloClientWithBaseUri.class, EchoResource.class, HelloClient.class))
            .withConfigurationResource("legacy-configuration-test-application.properties");

    @Inject
    RestClientsConfig configRoot;

    @Test
    void configurationShouldBeLoaded() {
        assertThat(configRoot.disableSmartProduces).isPresent();
        assertThat(configRoot.disableSmartProduces.get()).isTrue();
        assertThat(configRoot.multipartPostEncoderMode).isPresent();
        assertThat(configRoot.multipartPostEncoderMode.get()).isEqualTo("RFC3986");

        RestClientConfig clientConfig = RestClientConfig.load(io.quarkus.rest.client.reactive.HelloClient.class);
        assertThat(clientConfig.maxRedirects).isPresent();
        assertThat(clientConfig.maxRedirects.get()).isEqualTo(4);

        clientConfig = RestClientConfig.load("client-prefix");
        assertThat(clientConfig.maxRedirects).isPresent();
        assertThat(clientConfig.maxRedirects.get()).isEqualTo(4);
    }

}
