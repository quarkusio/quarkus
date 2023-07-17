package io.quarkus.restclient.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Set;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.test.QuarkusUnitTest;

public class GlobalConfigurationTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(EchoClient.class, EchoResource.class, MyResponseFilter.class,
                    MyHostnameVerifier.class))
            .withConfigurationResource("global-configuration-test-application.properties");

    @Inject
    RestClientsConfig configRoot;

    @RestClient
    EchoClient client;

    @Test
    void shouldHaveSingletonScope() {
        BeanManager beanManager = Arc.container().beanManager();
        Set<Bean<?>> beans = beanManager.getBeans(EchoClient.class, RestClient.LITERAL);
        Bean<?> resolvedBean = beanManager.resolve(beans);
        assertThat(resolvedBean.getScope()).isEqualTo(Singleton.class);
    }

    @Test
    void shouldRespond() {
        assertThat(client.echo("world")).contains("world");
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void checkGlobalConfigValues() {
        // global properties:
        assertThat(configRoot.disableSmartProduces.get()).isTrue();
        assertThat(configRoot.multipartPostEncoderMode.get()).isEqualTo("HTML5");
        assertThat(configRoot.disableContextualErrorMessages).isTrue();

        // global defaults for client specific properties:
        assertThat(configRoot.scope.get()).isEqualTo("Singleton");
        assertThat(configRoot.proxyAddress.get()).isEqualTo("host:123");
        assertThat(configRoot.proxyUser.get()).isEqualTo("proxyUser");
        assertThat(configRoot.proxyPassword.get()).isEqualTo("proxyPassword");
        assertThat(configRoot.nonProxyHosts.get()).isEqualTo("nonProxyHosts");
        assertThat(configRoot.connectTimeout).isEqualTo(2000);
        assertThat(configRoot.readTimeout).isEqualTo(2001);
        assertThat(configRoot.userAgent.get()).isEqualTo("agent");
        assertThat(configRoot.headers).isEqualTo(Collections.singletonMap("foo", "bar"));
        assertThat(configRoot.hostnameVerifier.get())
                .isEqualTo("io.quarkus.restclient.configuration.MyHostnameVerifier");
        assertThat(configRoot.connectionTTL.get()).isEqualTo(20000); // value in ms, will be converted to seconds
        assertThat(configRoot.connectionPoolSize.get()).isEqualTo(2);
        assertThat(configRoot.maxRedirects.get()).isEqualTo(2);
        assertThat(configRoot.followRedirects.get()).isTrue();
        assertThat(configRoot.providers.get())
                .isEqualTo("io.quarkus.restclient.configuration.MyResponseFilter");
        assertThat(configRoot.queryParamStyle.get()).isEqualTo(QueryParamStyle.MULTI_PAIRS);

        assertThat(configRoot.trustStore.get()).isEqualTo("/path");
        assertThat(configRoot.trustStorePassword.get()).isEqualTo("password");
        assertThat(configRoot.trustStoreType.get()).isEqualTo("JKS");
        assertThat(configRoot.keyStore.get()).isEqualTo("/path");
        assertThat(configRoot.keyStorePassword.get()).isEqualTo("password");
        assertThat(configRoot.keyStoreType.get()).isEqualTo("JKS");
    }

}
