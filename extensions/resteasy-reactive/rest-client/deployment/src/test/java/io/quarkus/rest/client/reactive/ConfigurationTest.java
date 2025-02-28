package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.rest.client.reactive.configuration.EchoResource;
import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.test.QuarkusUnitTest;

class ConfigurationTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(
                    jar -> jar.addClasses(HelloClientWithBaseUri.class, EchoResource.class, EchoClientWithEmptyPath.class))
            .withConfigurationResource("configuration-test-application.properties");

    @RestClient
    HelloClientWithBaseUri client;
    @RestClient
    EchoClientWithEmptyPath echoClientWithEmptyPath;
    @Inject
    RestClientsConfig restClientsConfig;

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
        RestClientsConfig.RestClientConfig clientConfig = restClientsConfig.getClient(HelloClientWithBaseUri.class);
        verifyClientConfig(clientConfig, true);

        clientConfig = restClientsConfig.getClient(ConfigKeyClient.class);
        verifyClientConfig(clientConfig, true);
        assertThat(clientConfig.proxyAddress().isPresent()).isTrue();
        assertThat(clientConfig.proxyAddress().get()).isEqualTo("localhost:8080");
        assertThat(clientConfig.headers()).containsOnly(entry("user-agent", "MP REST Client"), entry("foo", "bar"));

        clientConfig = restClientsConfig.getClient(QuotedConfigKeyClient.class);
        assertThat(clientConfig.url().isPresent()).isTrue();
        assertThat(clientConfig.url().get()).endsWith("/hello");
        assertThat(clientConfig.headers()).containsOnly(entry("foo", "bar"));

        clientConfig = restClientsConfig.getClient(MPConfigKeyClient.class);
        verifyClientConfig(clientConfig, false);
    }

    @Test
    void emptyPathAnnotationShouldWork() {
        assertThat(echoClientWithEmptyPath.echo("hello", "hello world")).isEqualTo("hello world");
    }

    private void verifyClientConfig(RestClientsConfig.RestClientConfig clientConfig, boolean checkExtraProperties) {
        assertTrue(clientConfig.url().isPresent());
        assertThat(clientConfig.url().get()).endsWith("/hello");
        assertTrue(clientConfig.providers().isPresent());
        assertThat(clientConfig.providers().get())
                .isEqualTo("io.quarkus.rest.client.reactive.HelloClientWithBaseUri$MyResponseFilter");
        assertTrue(clientConfig.connectTimeout().isPresent());
        assertThat(clientConfig.connectTimeout().get()).isEqualTo(5000);
        assertTrue(clientConfig.readTimeout().isPresent());
        assertThat(clientConfig.readTimeout().get()).isEqualTo(6000);
        assertTrue(clientConfig.followRedirects().isPresent());
        assertThat(clientConfig.followRedirects().get()).isEqualTo(true);
        assertTrue(clientConfig.queryParamStyle().isPresent());
        assertThat(clientConfig.queryParamStyle().get()).isEqualTo(QueryParamStyle.COMMA_SEPARATED);

        if (checkExtraProperties) {
            assertTrue(clientConfig.connectionTTL().isPresent());
            assertThat(clientConfig.connectionTTL().getAsInt()).isEqualTo(30000);
            assertTrue(clientConfig.connectionPoolSize().isPresent());
            assertThat(clientConfig.connectionPoolSize().getAsInt()).isEqualTo(10);
            assertTrue(clientConfig.keepAliveEnabled().isPresent());
            assertThat(clientConfig.keepAliveEnabled().get()).isFalse();
            assertTrue(clientConfig.maxRedirects().isPresent());
            assertThat(clientConfig.maxRedirects().getAsInt()).isEqualTo(5);
        }
    }

    @RegisterRestClient(configKey = "client-prefix")
    @Path("/")
    public interface ConfigKeyClient {
        @GET
        String get();
    }

    @RegisterRestClient(configKey = "quoted-client-prefix")
    @Path("/")
    public interface QuotedConfigKeyClient {
        @GET
        String get();
    }

    @RegisterRestClient(configKey = "mp-client-prefix")
    @Path("/")
    public interface MPConfigKeyClient {
        @GET
        String get();
    }
}
