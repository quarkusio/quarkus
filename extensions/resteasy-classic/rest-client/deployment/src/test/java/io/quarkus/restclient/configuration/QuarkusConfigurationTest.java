package io.quarkus.restclient.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.restclient.config.RestClientConfig;
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
    public void shouldConnect() {
        // TODO move to separate test
        //Assertions.assertEquals("Hello", client.echo("Hello"));
    }

    @Test
    void shouldHaveSingletonScope() {
        BeanManager beanManager = Arc.container().beanManager();
        Set<Bean<?>> beans = beanManager.getBeans(EchoClientWithConfigKey.class, RestClient.LITERAL);
        Bean<?> resolvedBean = beanManager.resolve(beans);
        Assertions.assertEquals(Singleton.class, resolvedBean.getScope());
    }

    @Test
    void configurationsShouldBeLoaded() {
        assertThat(configRoot.configs.size()).isEqualTo(5);

        verifyClientConfig(configRoot.configs.get("echo-client"), true);
        verifyClientConfig(configRoot.configs.get("io.quarkus.restclient.configuration.EchoClient"), true);
        verifyClientConfig(configRoot.configs.get("EchoClient"), true);
        verifyClientConfig(configRoot.configs.get("mp-client"), false); // non-standard properties cannot be set via MP style config
        verifyClientConfig(configRoot.configs.get("a.b.c.Client"), false);
    }

    void verifyClientConfig(RestClientConfig clientConfig, boolean verifyNonStandardProperties) {
        assertThat(clientConfig.url).isPresent();
        assertThat(clientConfig.url.get()).contains("localhost");
        assertThat(clientConfig.scope).isPresent();
        assertThat(clientConfig.scope.get()).isEqualTo("Singleton");
        assertThat(clientConfig.providers).isPresent();
        assertThat(clientConfig.providers.get())
                .isEqualTo("io.quarkus.restclient.configuration.MyResponseFilter");
        assertThat(clientConfig.connectTimeout).isPresent();
        assertThat(clientConfig.connectTimeout.get()).isEqualTo(5000);
        assertThat(clientConfig.readTimeout).isPresent();
        assertThat(clientConfig.readTimeout.get()).isEqualTo(6000);
        assertThat(clientConfig.followRedirects).isPresent();
        assertThat(clientConfig.followRedirects.get()).isEqualTo(true);
        assertThat(clientConfig.proxyAddress).isPresent();
        assertThat(clientConfig.proxyAddress.get()).isEqualTo("localhost:8080");
        assertThat(clientConfig.queryParamStyle).isPresent();
        assertThat(clientConfig.queryParamStyle.get()).isEqualTo(QueryParamStyle.COMMA_SEPARATED);
        assertThat(clientConfig.hostnameVerifier).isPresent();
        assertThat(clientConfig.hostnameVerifier.get())
                .isEqualTo("io.quarkus.restclient.configuration.MyHostnameVerifier");

        if (verifyNonStandardProperties) {
            assertThat(clientConfig.connectionTTL).isPresent();
            assertThat(clientConfig.connectionTTL.get()).isEqualTo(30000);
            assertThat(clientConfig.connectionPoolSize).isPresent();
            assertThat(clientConfig.connectionPoolSize.get()).isEqualTo(10);
        }
    }
}
