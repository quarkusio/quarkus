package io.quarkus.restclient.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.config.SmallRyeConfig;

public class RestClientConfigTest {

    @Test
    public void testLoadRestClientConfig() {
        SmallRyeConfig config = ConfigUtils.emptyConfigBuilder()
                .withMapping(RestClientsConfig.class)
                .withInterceptors(new RestClientNameFallbackConfigSourceInterceptor(
                        List.of(new RegisteredRestClient(RestClientConfigTest.class.getName(),
                                RestClientConfigTest.class.getSimpleName()))))
                .build();

        Optional<String> optionalValue = config.getOptionalValue("quarkus.rest-client.test-client.url", String.class);
        assertThat(optionalValue).isPresent();

        RestClientsConfig restClientsConfig = config.getConfigMapping(RestClientsConfig.class);

        RestClientsConfig.RestClientConfig configForKey = restClientsConfig.getClient("test-client");
        verifyConfig(configForKey);

        RestClientsConfig.RestClientConfig configForClassName = restClientsConfig.getClient(RestClientConfigTest.class);
        verifyConfig(configForClassName);
    }

    private void verifyConfig(RestClientsConfig.RestClientConfig config) {
        assertTrue(config.url().isPresent());
        assertThat(config.url().get()).isEqualTo("http://localhost:8080");
        assertTrue(config.uri().isPresent());
        assertThat(config.uri().get()).isEqualTo("http://localhost:8081");
        assertTrue(config.providers().isPresent());
        assertThat(config.providers().get()).isEqualTo("io.quarkus.restclient.configuration.MyResponseFilter");
        assertTrue(config.connectTimeout().isPresent());
        assertThat(config.connectTimeout().get()).isEqualTo(5000);
        assertTrue(config.readTimeout().isPresent());
        assertThat(config.readTimeout().get()).isEqualTo(6000);
        assertTrue(config.followRedirects().isPresent());
        assertThat(config.followRedirects().get()).isEqualTo(true);
        assertTrue(config.proxyAddress().isPresent());
        assertThat(config.proxyAddress().get()).isEqualTo("localhost:8080");
        assertTrue(config.queryParamStyle().isPresent());
        assertThat(config.queryParamStyle().get()).isEqualTo(QueryParamStyle.COMMA_SEPARATED);
        assertTrue(config.hostnameVerifier().isPresent());
        assertThat(config.hostnameVerifier().get()).isEqualTo("io.quarkus.restclient.configuration.MyHostnameVerifier");
        assertTrue(config.connectionTTL().isPresent());
        assertThat(config.connectionTTL().get()).isEqualTo(30000);
        assertTrue(config.connectionPoolSize().isPresent());
        assertThat(config.connectionPoolSize().get()).isEqualTo(10);
        assertTrue(config.maxChunkSize().isPresent());
        assertThat(config.maxChunkSize().get().asBigInteger()).isEqualTo(BigInteger.valueOf(1024));
    }
}
