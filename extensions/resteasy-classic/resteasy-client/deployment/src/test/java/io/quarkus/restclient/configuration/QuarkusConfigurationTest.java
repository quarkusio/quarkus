package io.quarkus.restclient.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.test.QuarkusUnitTest;

public class QuarkusConfigurationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(EchoResource.class, EchoClientWithConfigKey.class, MyResponseFilter.class,
                            MyHostnameVerifier.class))
            .withConfigurationResource("restclient-config-test-application.properties");

    @RestClient
    EchoClientWithConfigKey client;

    @Inject
    RestClientsConfig configRoot;

    @Test
    void shouldHaveSingletonScope() {
        BeanManager beanManager = Arc.container().beanManager();
        Set<Bean<?>> beans = beanManager.getBeans(EchoClientWithConfigKey.class, RestClient.LITERAL);
        Bean<?> resolvedBean = beanManager.resolve(beans);
        Assertions.assertEquals(Singleton.class, resolvedBean.getScope());
    }

    @Test
    void configurationsShouldBeLoaded() {
        verifyClientConfig(configRoot.clients().get("echo-client"), true);
        verifyClientConfig(configRoot.clients().get("io.quarkus.restclient.configuration.EchoClient"), true);
        verifyClientConfig(configRoot.clients().get("EchoClient"), true);
        verifyClientConfig(configRoot.clients().get("mp-client"), false); // non-standard properties cannot be set via MP style config
        verifyClientConfig(configRoot.clients().get("a.b.c.Client"), false);
    }

    void verifyClientConfig(RestClientsConfig.RestClientConfig clientConfig, boolean verifyNonStandardProperties) {
        assertTrue(clientConfig.url().isPresent());
        assertThat(clientConfig.url().get()).contains("localhost");
        assertTrue(clientConfig.providers().isPresent());
        assertThat(clientConfig.providers().get())
                .isEqualTo("io.quarkus.restclient.configuration.MyResponseFilter");
        assertTrue(clientConfig.connectTimeout().isPresent());
        assertThat(clientConfig.connectTimeout().get()).isEqualTo(5000);
        assertTrue(clientConfig.readTimeout().isPresent());
        assertThat(clientConfig.readTimeout().get()).isEqualTo(6000);
        assertTrue(clientConfig.followRedirects().isPresent());
        assertThat(clientConfig.followRedirects().get()).isEqualTo(true);
        assertTrue(clientConfig.proxyAddress().isPresent());
        assertThat(clientConfig.proxyAddress().get()).isEqualTo("localhost:8080");
        assertTrue(clientConfig.queryParamStyle().isPresent());
        assertThat(clientConfig.queryParamStyle().get()).isEqualTo(QueryParamStyle.COMMA_SEPARATED);
        assertTrue(clientConfig.hostnameVerifier().isPresent());
        assertThat(clientConfig.hostnameVerifier().get())
                .isEqualTo("io.quarkus.restclient.configuration.MyHostnameVerifier");

        if (verifyNonStandardProperties) {
            assertTrue(clientConfig.connectionTTL().isPresent());
            assertThat(clientConfig.connectionTTL().get()).isEqualTo(30000);
            assertTrue(clientConfig.connectionPoolSize().isPresent());
            assertThat(clientConfig.connectionPoolSize().get()).isEqualTo(10);
        }
    }
}
