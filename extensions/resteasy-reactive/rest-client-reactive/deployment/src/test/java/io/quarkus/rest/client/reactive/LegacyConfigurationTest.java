package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

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
                    .addClasses(HelloClientWithBaseUri.class, EchoResource.class))
            .withConfigurationResource("legacy-configuration-test-application.properties");

    @Inject
    RestClientsConfig configRoot;

    @Test
    void configurationShouldBeLoaded() {
        assertThat(configRoot.disableSmartProduces).isPresent();
        assertThat(configRoot.disableSmartProduces.get()).isTrue();
        assertThat(configRoot.multipartPostEncoderMode).isPresent();
        assertThat(configRoot.multipartPostEncoderMode.get()).isEqualTo("RFC3986");
        assertThat(configRoot.configs.size()).isEqualTo(2);

        RestClientConfig clientConfig = configRoot.configs.get("a.b.c.RestClient");
        assertThat(clientConfig.maxRedirects).isPresent();
        assertThat(clientConfig.maxRedirects.get()).isEqualTo(4);

        clientConfig = configRoot.configs.get("client-prefix");
        assertThat(clientConfig.maxRedirects).isPresent();
        assertThat(clientConfig.maxRedirects.get()).isEqualTo(4);
    }

}
