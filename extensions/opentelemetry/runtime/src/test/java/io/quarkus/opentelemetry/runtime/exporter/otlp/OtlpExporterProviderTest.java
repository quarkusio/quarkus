package io.quarkus.opentelemetry.runtime.exporter.otlp;

import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig.DEFAULT_GRPC_BASE_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import io.quarkus.opentelemetry.runtime.config.runtime.exporter.*;

class OtlpExporterProviderTest {

    @Test
    public void resolveTraceEndpoint_newWins() {
        assertEquals("http://localhost:2222/",
                OTelExporterRecorder.resolveTraceEndpoint(createOtlpExporterRuntimeConfig(
                        "http://localhost:1111/",
                        "http://localhost:2222/")));
    }

    @Test
    public void resolveTraceEndpoint_globalWins() {
        assertEquals("http://localhost:1111/",
                OTelExporterRecorder.resolveTraceEndpoint(createOtlpExporterRuntimeConfig(
                        "http://localhost:1111/",
                        DEFAULT_GRPC_BASE_URI)));
    }

    @Test
    public void resolveTraceEndpoint_legacyTraceWins() {
        assertEquals("http://localhost:2222/",
                OTelExporterRecorder.resolveTraceEndpoint(createOtlpExporterRuntimeConfig(
                        DEFAULT_GRPC_BASE_URI,
                        "http://localhost:2222/")));
    }

    @Test
    public void resolveTraceEndpoint_legacyGlobalWins() {
        assertEquals(DEFAULT_GRPC_BASE_URI,
                OTelExporterRecorder.resolveTraceEndpoint(createOtlpExporterRuntimeConfig(
                        DEFAULT_GRPC_BASE_URI,
                        null)));
    }

    @Test
    public void resolveTraceEndpoint_testIsSet() {
        assertEquals(DEFAULT_GRPC_BASE_URI,
                OTelExporterRecorder.resolveTraceEndpoint(createOtlpExporterRuntimeConfig(
                        null,
                        null)));
    }

    @Test
    public void resolveMetricEndpoint_newWins() {
        assertEquals("http://localhost:2222/",
                OTelExporterRecorder.resolveMetricEndpoint(createOtlpExporterRuntimeConfig(
                        "http://localhost:1111/",
                        "http://localhost:2222/")));
    }

    @Test
    public void resolveMetricEndpoint_globalWins() {
        assertEquals("http://localhost:1111/",
                OTelExporterRecorder.resolveMetricEndpoint(createOtlpExporterRuntimeConfig(
                        "http://localhost:1111/",
                        DEFAULT_GRPC_BASE_URI)));
    }

    @Test
    public void resolveMetricEndpoint_legacyTraceWins() {
        assertEquals("http://localhost:2222/",
                OTelExporterRecorder.resolveMetricEndpoint(createOtlpExporterRuntimeConfig(
                        DEFAULT_GRPC_BASE_URI,
                        "http://localhost:2222/")));
    }

    @Test
    public void resolveMetricEndpoint_legacyGlobalWins() {
        assertEquals(DEFAULT_GRPC_BASE_URI,
                OTelExporterRecorder.resolveMetricEndpoint(createOtlpExporterRuntimeConfig(
                        DEFAULT_GRPC_BASE_URI,
                        null)));
    }

    @Test
    public void resolveMetricEndpoint_testIsSet() {
        assertEquals(DEFAULT_GRPC_BASE_URI,
                OTelExporterRecorder.resolveMetricEndpoint(createOtlpExporterRuntimeConfig(
                        null,
                        null)));
    }

    private OtlpExporterRuntimeConfig createOtlpExporterRuntimeConfig(String exporterGlobal, String newTrace) {
        return new OtlpExporterRuntimeConfig() {
            @Override
            public Optional<String> endpoint() {
                return Optional.ofNullable(exporterGlobal);
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
                return null;
            }

            @Override
            public Optional<String> protocol() {
                return Optional.empty();
            }

            @Override
            public KeyCert keyCert() {
                return null;
            }

            @Override
            public TrustCert trustCert() {
                return null;
            }

            @Override
            public Optional<String> tlsConfigurationName() {
                return Optional.empty();
            }

            @Override
            public ProxyConfig proxyOptions() {
                return null;
            }

            @Override
            public OtlpExporterTracesConfig traces() {
                return new OtlpExporterTracesConfig() {
                    @Override
                    public Optional<String> endpoint() {
                        return Optional.ofNullable(newTrace);
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
                        return null;
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
                    public Optional<String> tlsConfigurationName() {
                        return Optional.empty();
                    }

                    @Override
                    public ProxyConfig proxyOptions() {
                        return new ProxyConfig() {
                            @Override
                            public boolean enabled() {
                                return false;
                            }

                            @Override
                            public Optional<String> username() {
                                return Optional.empty();
                            }

                            @Override
                            public Optional<String> password() {
                                return Optional.empty();
                            }

                            @Override
                            public OptionalInt port() {
                                return OptionalInt.empty();
                            }

                            @Override
                            public Optional<String> host() {
                                return Optional.empty();
                            }
                        };
                    }
                };
            }

            @Override
            public OtlpExporterMetricsConfig metrics() {
                return new OtlpExporterMetricsConfig() {
                    @Override
                    public Optional<String> temporalityPreference() {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<String> defaultHistogramAggregation() {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<String> endpoint() {
                        return Optional.ofNullable(newTrace);
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
                        return null;
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
                    public Optional<String> tlsConfigurationName() {
                        return Optional.empty();
                    }

                    @Override
                    public ProxyConfig proxyOptions() {
                        return null;
                    }
                };

            }

            @Override
            public OtlpExporterLogsConfig logs() {
                return new OtlpExporterLogsConfig() {
                    @Override
                    public Optional<String> endpoint() {
                        return Optional.ofNullable(newTrace);
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
                        return null;
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
                    public Optional<String> tlsConfigurationName() {
                        return Optional.empty();
                    }

                    @Override
                    public ProxyConfig proxyOptions() {
                        return null;
                    }
                };

            }
        };
    }
}
