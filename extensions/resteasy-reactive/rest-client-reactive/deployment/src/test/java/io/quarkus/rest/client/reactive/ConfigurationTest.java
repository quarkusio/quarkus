package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.rest.client.reactive.configuration.EchoResource;
import io.quarkus.restclient.config.RestClientConfig;
import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigurationTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloClientWithBaseUri.class, EchoResource.class))
            .withConfigurationResource("configuration-test-application.properties");

    @RestClient
    HelloClientWithBaseUri client;

    @Inject
    RestClientsConfig configRoot;

    @Test
    void shouldHaveSingletonScope() {
        BeanManager beanManager = Arc.container().beanManager();
        Set<Bean<?>> beans = beanManager.getBeans(HelloClientWithBaseUri.class, RestClient.LITERAL);
        Bean<?> resolvedBean = beanManager.resolve(beans);
        assertThat(resolvedBean.getScope()).isEqualTo(Singleton.class);
    }

    @Test
    void clientShouldRespond() {
        assertThat(client.echo("world!")).isEqualTo("hello, world!");
    }

    @Test
    void configurationShouldBeLoaded() {
        assertThat(configRoot.disableSmartProduces).isPresent();
        assertThat(configRoot.disableSmartProduces.get()).isTrue();
        assertThat(configRoot.multipartPostEncoderMode).isPresent();
        assertThat(configRoot.multipartPostEncoderMode.get()).isEqualTo("HTML5");
        assertThat(configRoot.configs.size()).isEqualTo(4);

        RestClientConfig clientConfig = configRoot.configs.get("io.quarkus.rest.client.reactive.HelloClientWithBaseUri");
        verifyClientConfig(clientConfig, true);

        clientConfig = configRoot.configs.get("client-prefix");
        verifyClientConfig(clientConfig, true);
        assertThat(clientConfig.proxyAddress.isPresent()).isTrue();
        assertThat(clientConfig.proxyAddress.get()).isEqualTo("localhost:8080");

        clientConfig = configRoot.configs.get("quoted-client-prefix");
        assertThat(clientConfig.url.isPresent()).isTrue();
        assertThat(clientConfig.url.get()).endsWith("/hello");

        clientConfig = configRoot.configs.get("mp-client-prefix");
        verifyClientConfig(clientConfig, false);
        assertThat(clientConfig.maxRedirects.isPresent()).isTrue();
        assertThat(clientConfig.maxRedirects.get()).isEqualTo(4);
    }

    private void verifyClientConfig(RestClientConfig clientConfig, boolean checkExtraProperties) {
        assertThat(clientConfig.url).isPresent();
        assertThat(clientConfig.url.get()).endsWith("/hello");
        assertThat(clientConfig.scope).isPresent();
        assertThat(clientConfig.scope.get()).isEqualTo("Singleton");
        assertThat(clientConfig.providers).isPresent();
        assertThat(clientConfig.providers.get())
                .isEqualTo("io.quarkus.rest.client.reactive.HelloClientWithBaseUri$MyResponseFilter");
        assertThat(clientConfig.connectTimeout).isPresent();
        assertThat(clientConfig.connectTimeout.get()).isEqualTo(5000);
        assertThat(clientConfig.readTimeout).isPresent();
        assertThat(clientConfig.readTimeout.get()).isEqualTo(6000);
        assertThat(clientConfig.followRedirects).isPresent();
        assertThat(clientConfig.followRedirects.get()).isEqualTo(true);
        assertThat(clientConfig.queryParamStyle).isPresent();
        assertThat(clientConfig.queryParamStyle.get()).isEqualTo(QueryParamStyle.COMMA_SEPARATED);
        assertThat(clientConfig.hostnameVerifier).isPresent();
        assertThat(clientConfig.hostnameVerifier.get())
                .isEqualTo("io.quarkus.rest.client.reactive.HelloClientWithBaseUri$MyHostnameVerifier");

        if (checkExtraProperties) {
            assertThat(clientConfig.connectionTTL).isPresent();
            assertThat(clientConfig.connectionTTL.get()).isEqualTo(30000);
            assertThat(clientConfig.connectionPoolSize).isPresent();
            assertThat(clientConfig.connectionPoolSize.get()).isEqualTo(10);
            assertThat(clientConfig.maxRedirects).isPresent();
            assertThat(clientConfig.maxRedirects.get()).isEqualTo(5);
        }
    }
}
