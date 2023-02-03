package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Set;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.rest.client.reactive.configuration.EchoResource;
import io.quarkus.restclient.config.RestClientConfig;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigurationTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(HelloClientWithBaseUri.class, EchoResource.class))
            .withConfigurationResource("configuration-test-application.properties");

    @RestClient
    HelloClientWithBaseUri client;

    @Test
    void shouldHaveSingletonScope() {
        BeanManager beanManager = Arc.container().beanManager();
        Set<Bean<?>> beans = beanManager.getBeans(HelloClientWithBaseUri.class, RestClient.LITERAL);
        Bean<?> resolvedBean = beanManager.resolve(beans);
        assertThat(resolvedBean.getScope()).isEqualTo(Singleton.class);
    }

    @Test
    void clientShouldRespond() {
        assertThat(client.echo("world")).isEqualTo("hi, world!");
    }

    @Test
    void checkClientSpecificConfigs() {
        RestClientConfig clientConfig = RestClientConfig.load(io.quarkus.rest.client.reactive.HelloClientWithBaseUri.class);
        verifyClientConfig(clientConfig, true);

        clientConfig = RestClientConfig.load("client-prefix");
        verifyClientConfig(clientConfig, true);
        assertThat(clientConfig.proxyAddress.isPresent()).isTrue();
        assertThat(clientConfig.proxyAddress.get()).isEqualTo("localhost:8080");
        assertThat(clientConfig.headers).containsOnly(entry("user-agent", "MP REST Client"), entry("foo", "bar"));

        clientConfig = RestClientConfig.load("quoted-client-prefix");
        assertThat(clientConfig.url.isPresent()).isTrue();
        assertThat(clientConfig.url.get()).endsWith("/hello");
        assertThat(clientConfig.headers).containsOnly(entry("foo", "bar"));

        clientConfig = RestClientConfig.load("mp-client-prefix");
        verifyClientConfig(clientConfig, false);
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
