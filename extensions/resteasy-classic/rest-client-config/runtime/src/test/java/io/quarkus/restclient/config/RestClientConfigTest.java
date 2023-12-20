package io.quarkus.restclient.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.util.Optional;

import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.junit.jupiter.api.Test;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class RestClientConfigTest {

    @Test
    public void testLoadRestClientConfig() throws IOException {
        SmallRyeConfig config = createMPConfig();

        Optional<String> optionalValue = config.getOptionalValue("quarkus.rest-client.test-client.url", String.class);
        assertThat(optionalValue).isPresent();

        RestClientConfig configForKey = RestClientConfig.load("test-client");
        verifyConfig(configForKey);
        RestClientConfig configForClassName = RestClientConfig.load(RestClientConfigTest.class);
        verifyConfig(configForClassName);
    }

    private void verifyConfig(RestClientConfig config) {
        assertThat(config.url).isPresent();
        assertThat(config.url.get()).isEqualTo("http://localhost:8080");
        assertThat(config.uri).isPresent();
        assertThat(config.uri.get()).isEqualTo("http://localhost:8081");
        assertThat(config.providers).isPresent();
        assertThat(config.providers.get()).isEqualTo("io.quarkus.restclient.configuration.MyResponseFilter");
        assertThat(config.connectTimeout).isPresent();
        assertThat(config.connectTimeout.get()).isEqualTo(5000);
        assertThat(config.readTimeout).isPresent();
        assertThat(config.readTimeout.get()).isEqualTo(6000);
        assertThat(config.followRedirects).isPresent();
        assertThat(config.followRedirects.get()).isEqualTo(true);
        assertThat(config.proxyAddress).isPresent();
        assertThat(config.proxyAddress.get()).isEqualTo("localhost:8080");
        assertThat(config.queryParamStyle).isPresent();
        assertThat(config.queryParamStyle.get()).isEqualTo(QueryParamStyle.COMMA_SEPARATED);
        assertThat(config.hostnameVerifier).isPresent();
        assertThat(config.hostnameVerifier.get()).isEqualTo("io.quarkus.restclient.configuration.MyHostnameVerifier");
        assertThat(config.connectionTTL).isPresent();
        assertThat(config.connectionTTL.get()).isEqualTo(30000);
        assertThat(config.connectionPoolSize).isPresent();
        assertThat(config.connectionPoolSize.get()).isEqualTo(10);
        assertThat(config.maxChunkSize.get().asBigInteger()).isEqualTo(BigInteger.valueOf(1024));
    }

    private static SmallRyeConfig createMPConfig() throws IOException {
        SmallRyeConfigBuilder configBuilder = new SmallRyeConfigBuilder().addDefaultInterceptors();
        URL propertyFile = RestClientConfigTest.class.getClassLoader().getResource("application.properties");
        assertThat(propertyFile).isNotNull();
        configBuilder.withSources(new PropertiesConfigSource(propertyFile));
        return configBuilder.build();
    }
}
