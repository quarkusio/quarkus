package io.quarkus.restclient.config;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.junit.jupiter.api.Test;

import io.quarkus.restclient.config.RestClientsConfig.RestClientConfig;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;
import io.smallrye.config.common.MapBackedConfigSource;

class RestClientConfigTest {
    @Test
    void loadRestClientConfig() {
        SmallRyeConfig config = ConfigUtils.emptyConfigBuilder()
                .withMapping(RestClientsConfig.class)
                .withCustomizers(new SmallRyeConfigBuilderCustomizer() {
                    @Override
                    public void configBuilder(final SmallRyeConfigBuilder builder) {
                        new AbstractRestClientConfigBuilder() {
                            @Override
                            public List<RegisteredRestClient> getRestClients() {
                                return List.of(
                                        new RegisteredRestClient(RestClientConfigTest.class.getName(),
                                                RestClientConfigTest.class.getSimpleName()));
                            }
                        }.configBuilder(builder);
                    }
                })
                .build();

        Optional<String> optionalValue = config.getOptionalValue("quarkus.rest-client.test-client.url", String.class);
        assertThat(optionalValue).isPresent();

        RestClientsConfig restClientsConfig = config.getConfigMapping(RestClientsConfig.class);

        RestClientConfig configForKey = restClientsConfig.getClient("test-client");
        verifyConfig(configForKey);

        RestClientConfig configForClassName = restClientsConfig.getClient(RestClientConfigTest.class);
        verifyConfig(configForClassName);
    }

    private void verifyConfig(RestClientConfig config) {
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

    @Test
    void restClientConfigKey() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDiscoveredConverters()
                .withMappingIgnore("quarkus.**")
                .withMapping(RestClientsConfig.class)
                .withSources(new MapBackedConfigSource("", Map.of(
                        "quarkus.rest-client.simple.uri", "http://localhost:8081",
                        "quarkus.rest-client.simple.url", "http://localhost:8081",
                        "quarkus.rest-client.\"quoted\".uri", "http://localhost:8081",
                        "quarkus.rest-client.\"quoted\".url", "http://localhost:8081",
                        "quarkus.rest-client.mixed.uri", "http://localhost:8081",
                        "quarkus.rest-client.\"mixed\".url", "http://localhost:8081",
                        "quarkus.rest-client.da-shed.uri", "http://localhost:8081",
                        "quarkus.rest-client.da-shed.url", "http://localhost:8081",
                        "quarkus.rest-client.\"segments.paths\".uri", "http://localhost:8081",
                        "quarkus.rest-client.\"segments.paths\".url", "http://localhost:8081")) {
                })
                .withCustomizers(new SmallRyeConfigBuilderCustomizer() {
                    @Override
                    public void configBuilder(final SmallRyeConfigBuilder builder) {
                        new AbstractRestClientConfigBuilder() {
                            @Override
                            public List<RegisteredRestClient> getRestClients() {
                                return List.of(
                                        new RegisteredRestClient("dummy", "dummy", "simple"),
                                        new RegisteredRestClient("dummy", "dummy", "quoted"),
                                        new RegisteredRestClient("dummy", "dummy", "mixed"),
                                        new RegisteredRestClient("dummy", "dummy", "da-shed"),
                                        new RegisteredRestClient("dummy", "dummy", "segments.paths"));
                            }
                        }.configBuilder(builder);
                    }
                })
                .build();

        Set<String> names = StreamSupport.stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        assertTrue(names.contains("quarkus.rest-client.simple.uri"));
        assertTrue(names.contains("quarkus.rest-client.\"simple\".uri"));
        assertTrue(names.contains("quarkus.rest-client.quoted.uri"));
        assertTrue(names.contains("quarkus.rest-client.\"quoted\".uri"));
        assertTrue(names.contains("quarkus.rest-client.mixed.uri"));
        assertTrue(names.contains("quarkus.rest-client.\"mixed\".uri"));
        assertTrue(names.contains("quarkus.rest-client.da-shed.uri"));
        assertTrue(names.contains("quarkus.rest-client.\"da-shed\".uri"));
        assertTrue(names.contains("quarkus.rest-client.\"segments.paths\".uri"));
        assertFalse(names.contains("quarkus.rest-client.segments.paths.uri"));

        RestClientsConfig restClientsConfig = config.getConfigMapping(RestClientsConfig.class);

        RestClientConfig simple = restClientsConfig.getClient("simple");
        assertTrue(simple.uri().isPresent());
        assertEquals("http://localhost:8081", simple.uri().get());
        assertTrue(simple.url().isPresent());
        assertEquals("http://localhost:8081", simple.url().get());

        RestClientConfig quoted = restClientsConfig.getClient("quoted");
        assertTrue(quoted.uri().isPresent());
        assertEquals("http://localhost:8081", quoted.uri().get());
        assertTrue(quoted.url().isPresent());
        assertEquals("http://localhost:8081", quoted.url().get());

        RestClientConfig mixed = restClientsConfig.getClient("mixed");
        assertTrue(mixed.uri().isPresent());
        assertEquals("http://localhost:8081", mixed.uri().get());
        assertTrue(mixed.url().isPresent());
        assertEquals("http://localhost:8081", mixed.url().get());

        RestClientConfig dashed = restClientsConfig.getClient("da-shed");
        assertTrue(dashed.uri().isPresent());
        assertEquals("http://localhost:8081", dashed.uri().get());
        assertTrue(dashed.url().isPresent());
        assertEquals("http://localhost:8081", dashed.url().get());

        RestClientConfig segments = restClientsConfig.getClient("segments.paths");
        assertTrue(segments.uri().isPresent());
        assertEquals("http://localhost:8081", segments.uri().get());
        assertTrue(segments.url().isPresent());
        assertEquals("http://localhost:8081", segments.url().get());
    }
}
