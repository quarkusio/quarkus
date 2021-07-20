package io.quarkus.restclient.configuration;

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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.restclient.config.RestClientConfig;
import io.quarkus.restclient.config.RestClientConfigRoot;
import io.quarkus.test.QuarkusUnitTest;

public class QuarkusConfigurationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(EchoResource.class, EchoClient.class))
            .withConfigurationResource("quarkus-config-test-application.properties");

    @RestClient
    EchoClient client;

    @Inject
    RestClientConfigRoot configRoot;

    @Test
    public void shouldConnect() {
        Assertions.assertEquals("Hello", client.echo("Hello"));
    }

    @Test
    void shouldHaveSingletonScope() {
        BeanManager beanManager = Arc.container().beanManager();
        Set<Bean<?>> beans = beanManager.getBeans(EchoClient.class, RestClient.LITERAL);
        Bean<?> resolvedBean = beanManager.resolve(beans);
        Assertions.assertEquals(Singleton.class, resolvedBean.getScope());
    }

    @Test
    void configurationShouldBeLoaded() {
        assertThat(configRoot.configs.size()).isEqualTo(1);

        RestClientConfig clientConfig = configRoot.configs.get("my-client");
        assertThat(clientConfig.url).isPresent();
        assertThat(clientConfig.url.get()).contains("localhost");
        assertThat(clientConfig.scope).isPresent();
        assertThat(clientConfig.scope.get()).isEqualTo("Singleton");
        assertThat(clientConfig.providers).isPresent();
        assertThat(clientConfig.providers.get())
                .isEqualTo("io.quarkus.restclient.configuration.EchoClient$MyResponseFilter");
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
                .isEqualTo("io.quarkus.restclient.configuration.EchoClient$MyHostnameVerifier");
        assertThat(clientConfig.connectionTTL).isPresent();
        assertThat(clientConfig.connectionTTL.get()).isEqualTo(30000);
        assertThat(clientConfig.connectionPoolSize).isPresent();
        assertThat(clientConfig.connectionPoolSize.get()).isEqualTo(10);
    }
}
