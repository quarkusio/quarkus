package io.quarkus.opentelemetry.runtime.exporter.otlp;

import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig.DEFAULT_GRPC_BASE_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.opentelemetry.runtime.config.runtime.exporter.CompressionType;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterTracesConfig;

class OtlpExporterProviderTest {

    @Test
    public void resolveEndpoint_legacyWins() {
        assertEquals("http://localhost:1111/",
                OtlpRecorder.resolveEndpoint(createOtlpExporterRuntimeConfig(
                        DEFAULT_GRPC_BASE_URI,
                        "http://localhost:1111/",
                        "http://localhost:2222/")));
    }

    @Test
    public void resolveEndpoint_newWins() {
        assertEquals("http://localhost:2222/",
                OtlpRecorder.resolveEndpoint(createOtlpExporterRuntimeConfig(
                        "http://localhost:1111/",
                        DEFAULT_GRPC_BASE_URI,
                        "http://localhost:2222/")));
    }

    @Test
    public void resolveEndpoint_globalWins() {
        assertEquals("http://localhost:1111/",
                OtlpRecorder.resolveEndpoint(createOtlpExporterRuntimeConfig(
                        "http://localhost:1111/",
                        DEFAULT_GRPC_BASE_URI,
                        DEFAULT_GRPC_BASE_URI)));
    }

    @Test
    public void resolveEndpoint_legacyTraceWins() {
        assertEquals("http://localhost:2222/",
                OtlpRecorder.resolveEndpoint(createOtlpExporterRuntimeConfig(
                        DEFAULT_GRPC_BASE_URI,
                        null,
                        "http://localhost:2222/")));
    }

    @Test
    public void resolveEndpoint_legacyGlobalWins() {
        assertEquals(DEFAULT_GRPC_BASE_URI,
                OtlpRecorder.resolveEndpoint(createOtlpExporterRuntimeConfig(
                        DEFAULT_GRPC_BASE_URI,
                        null,
                        null)));
    }

    @Test
    public void resolveEndpoint_testIsSet() {
        assertEquals(DEFAULT_GRPC_BASE_URI,
                OtlpRecorder.resolveEndpoint(createOtlpExporterRuntimeConfig(
                        null,
                        null,
                        null)));
    }

    private OtlpExporterRuntimeConfig createOtlpExporterRuntimeConfig(String exporterGlobal, String legacyTrace,
            String newTrace) {
        return new OtlpExporterRuntimeConfig() {
            @Override
            public Optional<String> endpoint() {
                return Optional.ofNullable(exporterGlobal);
            }

            @Override
            public OtlpExporterTracesConfig traces() {
                return new OtlpExporterTracesConfig() {
                    @Override
                    public Optional<String> endpoint() {
                        return Optional.ofNullable(newTrace);
                    }

                    @Override
                    public Optional<String> legacyEndpoint() {
                        return Optional.ofNullable(legacyTrace);
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
                };
            }
        };
    }
}
