package io.quarkus.opentelemetry.runtime.exporter.otlp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import io.quarkus.opentelemetry.runtime.config.runtime.exporter.CompressionType;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterTracesConfig;
import io.quarkus.runtime.TlsConfig;
import io.vertx.core.http.HttpClientOptions;

class HttpClientOptionsConsumerTest {

    @Test
    void testNoProxy() {
        OTelExporterRecorder.HttpClientOptionsConsumer consumer = new OTelExporterRecorder.HttpClientOptionsConsumer(
                createExporterConfig(false),
                URI.create("http://localhost:4317"),
                createTLSConfig());

        HttpClientOptions httpClientOptions = new HttpClientOptions();
        consumer.accept(httpClientOptions);
        assertThat(httpClientOptions.getProxyOptions(), nullValue());
    }

    @Test
    void testWithProxy() {
        OTelExporterRecorder.HttpClientOptionsConsumer consumer = new OTelExporterRecorder.HttpClientOptionsConsumer(
                createExporterConfig(true),
                URI.create("http://localhost:4317"),
                createTLSConfig());

        HttpClientOptions httpClientOptions = new HttpClientOptions();
        consumer.accept(httpClientOptions);
        assertThat(httpClientOptions.getProxyOptions(), notNullValue());
        assertThat(httpClientOptions.getProxyOptions().getHost(), is("proxy-address"));
        assertThat(httpClientOptions.getProxyOptions().getPort(), is(9999));
        assertThat(httpClientOptions.getProxyOptions().getUsername(), is("proxy-username"));
        assertThat(httpClientOptions.getProxyOptions().getPassword(), is("proxy-password"));
    }

    private TlsConfig createTLSConfig() {
        return new TlsConfig();
    }

    private OtlpExporterTracesConfig createExporterConfig(final boolean isEnabled) {
        return new OtlpExporterTracesConfig() {
            @Override
            public Optional<String> endpoint() {
                return Optional.of("http://localhost:4317");
            }

            @Override
            public Optional<String> legacyEndpoint() {
                return Optional.empty();
            }

            @Override
            public Optional<List<String>> headers() {
                return Optional.empty();
            }

            @Override
            public Optional<CompressionType> compression() {
                return Optional.empty();
            }

            @Override
            public Duration timeout() {
                return Duration.ofMillis(100);
            }

            @Override
            public Optional<String> protocol() {
                return Optional.empty();
            }

            @Override
            public KeyCert keyCert() {
                return new KeyCert() {
                    @Override
                    public Optional<List<String>> keys() {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<List<String>> certs() {
                        return Optional.empty();
                    }
                };
            }

            @Override
            public TrustCert trustCert() {
                return new TrustCert() {
                    @Override
                    public Optional<List<String>> certs() {
                        return Optional.empty();
                    }
                };
            }

            @Override
            public ProxyConfig proxyOptions() {
                return new ProxyConfig() {
                    @Override
                    public boolean enabled() {
                        return isEnabled;
                    }

                    @Override
                    public Optional<String> username() {
                        return Optional.of("proxy-username");
                    }

                    @Override
                    public Optional<String> password() {
                        return Optional.of("proxy-password");
                    }

                    @Override
                    public OptionalInt port() {
                        return OptionalInt.of(9999);
                    }

                    @Override
                    public Optional<String> host() {
                        return Optional.of("proxy-address");
                    }
                };
            }
        };
    }
}
