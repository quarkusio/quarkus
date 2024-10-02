package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.configuration.EchoResource;
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
        assertTrue(configRoot.multipartPostEncoderMode().isPresent());
        assertThat(configRoot.multipartPostEncoderMode().get()).isEqualTo("RFC3986");

        RestClientsConfig.RestClientConfig clientConfig = configRoot.getClient(HelloClient.class);
        assertTrue(clientConfig.maxRedirects().isPresent());
        assertThat(clientConfig.maxRedirects().get()).isEqualTo(4);

        clientConfig = configRoot.getClient("client-prefix");
        assertTrue(clientConfig.maxRedirects().isPresent());
        assertThat(clientConfig.maxRedirects().get()).isEqualTo(4);
    }
}
