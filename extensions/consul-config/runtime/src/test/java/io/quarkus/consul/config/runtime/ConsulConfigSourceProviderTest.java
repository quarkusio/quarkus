package io.quarkus.consul.config.runtime;

import static io.quarkus.consul.config.runtime.ResponseUtil.emptyResponse;
import static io.quarkus.consul.config.runtime.ResponseUtil.validResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

class ConsulConfigSourceProviderTest {

    private static final int EXPECTED_ORDINAL = 270;

    @Test
    void testEmptyKeys() throws IOException {
        ConsulConfig config = defaultConfig();

        ConsulConfigGateway mockGateway = mock(ConsulConfigGateway.class);
        ConsulConfigSourceProvider sut = new ConsulConfigSourceProvider(config, mockGateway);

        Iterable<ConfigSource> configSources = sut.getConfigSources(null);
        assertThat(configSources).isEmpty();

        // no interactions with Consul should have taken place
        verify(mockGateway, never()).getValue(anyString());
    }

    @Test
    void testWithMissingKeysAndFailureConfigured() throws IOException {
        ConsulConfig config = defaultConfig();
        config.rawValueKeys = keyValues("some/first", "some/second");
        config.failOnMissingKey = true;

        ConsulConfigGateway mockGateway = mock(ConsulConfigGateway.class);
        // make sure the first is is properly resolved
        when(mockGateway.getValue("some/first")).thenReturn(validResponse("some/first", "whatever"));
        // make sure the second is not resolved
        when(mockGateway.getValue("some/second")).thenReturn(emptyResponse());

        ConsulConfigSourceProvider sut = new ConsulConfigSourceProvider(config, mockGateway);

        assertThatThrownBy(() -> {
            sut.getConfigSources(null);
        }).isInstanceOf(RuntimeException.class).hasMessageContaining("some/second");

        //both of the keys should have been resolved because we resolve keys in the order they were given by the user
        verify(mockGateway, times(1)).getValue("some/first");
        verify(mockGateway, times(1)).getValue("some/second");
    }

    @Test
    void testWithMissingKeysAndIgnoreFailureConfigured() throws IOException {
        ConsulConfig config = defaultConfig();
        config.rawValueKeys = keyValues("some/first", "some/second", "some/third");
        config.failOnMissingKey = false;

        ConsulConfigGateway mockGateway = mock(ConsulConfigGateway.class);
        // make sure the first is is properly resolved
        when(mockGateway.getValue("some/first")).thenReturn(validResponse("some/first", "whatever"));
        // make sure the second is not resolved
        when(mockGateway.getValue("some/second")).thenReturn(emptyResponse());
        // make sure the third is is properly resolved
        when(mockGateway.getValue("some/third")).thenReturn(validResponse("some/third", "other"));

        ConsulConfigSourceProvider sut = new ConsulConfigSourceProvider(config, mockGateway);

        Iterable<ConfigSource> configSources = sut.getConfigSources(null);
        assertThat(configSources).hasSize(2);
        assertThat(configSources).filteredOn(c -> c.getName().contains("first")).singleElement().satisfies(c -> {
            assertThat(c.getOrdinal()).isEqualTo(EXPECTED_ORDINAL);
            assertThat(c.getProperties()).containsOnly(entry("some.first", "whatever"));
        });
        assertThat(configSources).filteredOn(c -> c.getName().contains("third")).singleElement().satisfies(c -> {
            assertThat(c.getOrdinal()).isEqualTo(EXPECTED_ORDINAL);
            assertThat(c.getProperties()).containsOnly(entry("some.third", "other"));
        });

        //all keys should have been resolved because we resolve keys in the order they were given by the user
        verify(mockGateway, times(1)).getValue("some/first");
        verify(mockGateway, times(1)).getValue("some/second");
        verify(mockGateway, times(1)).getValue("some/third");
    }

    @Test
    void testRawKeysWithoutPrefix() throws IOException {
        ConsulConfig config = defaultConfig();
        config.rawValueKeys = keyValues("greeting/message", "greeting/name");

        ConsulConfigGateway mockGateway = mock(ConsulConfigGateway.class);
        when(mockGateway.getValue("greeting/message"))
                .thenReturn(validResponse("greeting/message", "hello"));
        when(mockGateway.getValue("greeting/name"))
                .thenReturn(validResponse("greeting/name", "quarkus"));

        ConsulConfigSourceProvider sut = new ConsulConfigSourceProvider(config, mockGateway);

        Iterable<ConfigSource> configSources = sut.getConfigSources(null);
        assertThat(configSources).hasSize(2);
        assertThat(configSources).filteredOn(c -> c.getName().contains("message")).singleElement().satisfies(c -> {
            assertThat(c.getOrdinal()).isEqualTo(EXPECTED_ORDINAL);
            assertThat(c.getProperties()).containsOnly(entry("greeting.message", "hello"));
        });
        assertThat(configSources).filteredOn(c -> c.getName().contains("name")).singleElement().satisfies(c -> {
            assertThat(c.getOrdinal()).isEqualTo(EXPECTED_ORDINAL);
            assertThat(c.getProperties()).containsOnly(entry("greeting.name", "quarkus"));
        });

        verify(mockGateway, times(1)).getValue("greeting/message");
        verify(mockGateway, times(1)).getValue("greeting/name");
    }

    @Test
    void testRawKeysWithPrefix() throws IOException {
        ConsulConfig config = defaultConfig();
        config.prefix = Optional.of("whatever");
        config.rawValueKeys = keyValues("greeting/message", "greeting/name");

        ConsulConfigGateway mockGateway = mock(ConsulConfigGateway.class);
        when(mockGateway.getValue("whatever/greeting/message"))
                .thenReturn(validResponse("whatever/greeting/message", "hello"));
        when(mockGateway.getValue("whatever/greeting/name"))
                .thenReturn(validResponse("whatever/greeting/name", "quarkus"));

        ConsulConfigSourceProvider sut = new ConsulConfigSourceProvider(config, mockGateway);

        Iterable<ConfigSource> configSources = sut.getConfigSources(null);
        assertThat(configSources).hasSize(2);
        assertThat(configSources).filteredOn(c -> c.getName().contains("message")).singleElement().satisfies(c -> {
            assertThat(c.getOrdinal()).isEqualTo(EXPECTED_ORDINAL);
            assertThat(c.getProperties()).containsOnly(entry("greeting.message", "hello"));
        });
        assertThat(configSources).filteredOn(c -> c.getName().contains("name")).singleElement().satisfies(c -> {
            assertThat(c.getOrdinal()).isEqualTo(EXPECTED_ORDINAL);
            assertThat(c.getProperties()).containsOnly(entry("greeting.name", "quarkus"));
        });

        verify(mockGateway, times(1)).getValue("whatever/greeting/message");
        verify(mockGateway, times(1)).getValue("whatever/greeting/name");
    }

    @Test
    void testPropertiesKeysWithoutPrefix() throws IOException {
        ConsulConfig config = defaultConfig();
        config.propertiesValueKeys = keyValues("first", "second");

        ConsulConfigGateway mockGateway = mock(ConsulConfigGateway.class);
        when(mockGateway.getValue("first"))
                .thenReturn(validResponse("first", "greeting.message=hi\ngreeting.name=quarkus"));
        when(mockGateway.getValue("second"))
                .thenReturn(validResponse("second", "other.key=value"));

        ConsulConfigSourceProvider sut = new ConsulConfigSourceProvider(config, mockGateway);

        Iterable<ConfigSource> configSources = sut.getConfigSources(null);
        assertThat(configSources).hasSize(2);
        assertThat(configSources).filteredOn(c -> c.getName().contains("first")).singleElement().satisfies(c -> {
            assertThat(c.getOrdinal()).isEqualTo(EXPECTED_ORDINAL);
            assertThat(c.getProperties()).containsOnly(entry("greeting.message", "hi"),
                    entry("greeting.name", "quarkus"));
        });
        assertThat(configSources).filteredOn(c -> c.getName().contains("second")).singleElement().satisfies(c -> {
            assertThat(c.getOrdinal()).isEqualTo(EXPECTED_ORDINAL);
            assertThat(c.getProperties()).containsOnly(entry("other.key", "value"));
        });

        verify(mockGateway, times(1)).getValue("first");
        verify(mockGateway, times(1)).getValue("second");
    }

    @Test
    void testPropertiesKeysWithPrefix() throws IOException {
        ConsulConfig config = defaultConfig();
        config.prefix = Optional.of("config");
        config.propertiesValueKeys = keyValues("first", "second");

        ConsulConfigGateway mockGateway = mock(ConsulConfigGateway.class);
        when(mockGateway.getValue("config/first"))
                .thenReturn(validResponse("config/first", "greeting.message=hi\ngreeting.name=quarkus"));
        when(mockGateway.getValue("config/second"))
                .thenReturn(validResponse("config/second", "other.key=value"));

        ConsulConfigSourceProvider sut = new ConsulConfigSourceProvider(config, mockGateway);

        Iterable<ConfigSource> configSources = sut.getConfigSources(null);
        assertThat(configSources).hasSize(2);
        assertThat(configSources).filteredOn(c -> c.getName().contains("first")).singleElement().satisfies(c -> {
            assertThat(c.getOrdinal()).isEqualTo(EXPECTED_ORDINAL);
            assertThat(c.getProperties()).containsOnly(entry("greeting.message", "hi"),
                    entry("greeting.name", "quarkus"));
        });
        assertThat(configSources).filteredOn(c -> c.getName().contains("second")).singleElement().satisfies(c -> {
            assertThat(c.getOrdinal()).isEqualTo(EXPECTED_ORDINAL);
            assertThat(c.getProperties()).containsOnly(entry("other.key", "value"));
        });

        verify(mockGateway, times(1)).getValue("config/first");
        verify(mockGateway, times(1)).getValue("config/second");
    }

    private ConsulConfig defaultConfig() {
        ConsulConfig config = new ConsulConfig();
        config.enabled = true;
        config.failOnMissingKey = true;
        config.rawValueKeys = Optional.empty();
        config.propertiesValueKeys = Optional.empty();
        config.prefix = Optional.empty();
        config.agent = new ConsulConfig.AgentConfig();
        config.agent.readTimeout = Duration.ofSeconds(10);
        config.agent.connectionTimeout = Duration.ofSeconds(10);
        return config;
    }

    private Optional<List<String>> keyValues(String... keys) {
        return Optional.of(Arrays.asList(keys));
    }

}
