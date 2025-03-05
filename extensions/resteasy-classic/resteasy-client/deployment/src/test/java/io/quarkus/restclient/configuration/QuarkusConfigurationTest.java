package io.quarkus.restclient.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertEquals(1, configRoot.clients().size());
        verifyClientConfig(configRoot.clients().get(EchoClientWithConfigKey.class.getName()), true);
        assertFalse(configRoot.clients().containsKey("echo-client"));
        assertFalse(configRoot.clients().containsKey("EchoClient"));
        assertFalse(configRoot.clients().containsKey("EchoClientWithConfigKey"));
        assertFalse(configRoot.clients().containsKey("mp-client"));
        assertFalse(configRoot.clients().containsKey("a.b.c.Client"));
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
            assertThat(clientConfig.connectionTTL().getAsInt()).isEqualTo(30000);
            assertTrue(clientConfig.connectionPoolSize().isPresent());
            assertThat(clientConfig.connectionPoolSize().getAsInt()).isEqualTo(10);
        }
    }
}
