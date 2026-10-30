package io.quarkus.opentelemetry.runtime.exporter.otlp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterTracesConfig;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.config.ConfigInstanceBuilder;
import io.vertx.core.http.HttpClientOptions;

class HttpClientOptionsConsumerTest {

    @Test
    void testNoProxy() {
        OTelExporterRecorder.HttpClientOptionsConsumer consumer = new OTelExporterRecorder.HttpClientOptionsConsumer(
                createExporterConfig(false),
                URI.create("http://localhost:4317"),
                new NoopTlsConfigurationRegistry());

        HttpClientOptions httpClientOptions = new HttpClientOptions();
        consumer.accept(httpClientOptions);
        assertThat(httpClientOptions.getProxyOptions(), nullValue());
    }

    @Test
    void testWithProxy() {
        OTelExporterRecorder.HttpClientOptionsConsumer consumer = new OTelExporterRecorder.HttpClientOptionsConsumer(
                createExporterConfig(true),
                URI.create("http://localhost:4317"),
                new NoopTlsConfigurationRegistry());

        HttpClientOptions httpClientOptions = new HttpClientOptions();
        consumer.accept(httpClientOptions);
        assertThat(httpClientOptions.getProxyOptions(), notNullValue());
        assertThat(httpClientOptions.getProxyOptions().getHost(), is("proxy-address"));
        assertThat(httpClientOptions.getProxyOptions().getPort(), is(9999));
        assertThat(httpClientOptions.getProxyOptions().getUsername(), is("proxy-username"));
        assertThat(httpClientOptions.getProxyOptions().getPassword(), is("proxy-password"));
    }

    @Test
    void testTrustAllLogsWarning() {
        Logger logger = Logger.getLogger(OTelExporterRecorder.class.getName());
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        logger.addHandler(handler);
        try {
            OTelExporterRecorder.HttpClientOptionsConsumer consumer = new OTelExporterRecorder.HttpClientOptionsConsumer(
                    createExporterConfig(false),
                    URI.create("https://localhost:4317"),
                    createTrustAllRegistry());

            HttpClientOptions httpClientOptions = new HttpClientOptions();
            consumer.accept(httpClientOptions);

            assertThat(httpClientOptions.isTrustAll(), is(true));
            assertThat(httpClientOptions.isVerifyHost(), is(false));
            assertThat(records.stream()
                    .anyMatch(r -> r.getMessage().contains("trustAll=true") &&
                            r.getMessage().contains("This should not be used in production.") &&
                            r.getLevel().equals(java.util.logging.Level.WARNING)),
                    is(true));
        } finally {
            logger.removeHandler(handler);
        }
    }

    private OtlpExporterTracesConfig createExporterConfig(final boolean isEnabled) {
        return ConfigInstanceBuilder.forInterface(OtlpExporterTracesConfig.class)
                .withOptional(OtlpExporterConfig::endpoint, "http://localhost:4317")
                .with(OtlpExporterConfig::timeout, Duration.ofMillis(100))
                .with(OtlpExporterConfig::proxyOptions, ConfigInstanceBuilder.forInterface(OtlpExporterConfig.ProxyConfig.class)
                        .with(OtlpExporterConfig.ProxyConfig::enabled, isEnabled)
                        .withOptional(OtlpExporterConfig.ProxyConfig::username, "proxy-username")
                        .withOptional(OtlpExporterConfig.ProxyConfig::password, "proxy-password")
                        .withOptional(OtlpExporterConfig.ProxyConfig::port, 9999)
                        .withOptional(OtlpExporterConfig.ProxyConfig::host, "proxy-address")
                        .build())
                .build();
    }

    private static class NoopTlsConfigurationRegistry implements TlsConfigurationRegistry {
        @Override
        public Optional<TlsConfiguration> get(String name) {
            return Optional.empty();
        }

        @Override
        public Optional<TlsConfiguration> getDefault() {
            return Optional.empty();
        }

        @Override
        public void register(String name, TlsConfiguration configuration) {

        }
    }

    private static TlsConfigurationRegistry createTrustAllRegistry() {
        TlsConfiguration tlsConfig = Mockito.mock(TlsConfiguration.class);
        Mockito.when(tlsConfig.isTrustAll()).thenReturn(true);

        TlsConfigurationRegistry registry = Mockito.mock(TlsConfigurationRegistry.class);
        Mockito.when(registry.getDefault()).thenReturn(Optional.of(tlsConfig));
        Mockito.when(registry.get(Mockito.anyString())).thenReturn(Optional.empty());
        return registry;
    }
}
