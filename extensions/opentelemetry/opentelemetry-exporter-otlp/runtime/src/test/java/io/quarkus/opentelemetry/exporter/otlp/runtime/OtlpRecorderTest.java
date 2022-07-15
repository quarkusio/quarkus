package io.quarkus.opentelemetry.exporter.otlp.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OtlpRecorderTest {

    @ParameterizedTest()
    @CsvSource({
            "http://localhost:1234/,v1/traces,,http://localhost:1234/v1/traces", //new
            ",v1/traces,,", //invalid
            "http://localhost:1234/,v1/traces,http://localhost:1234/old/traces,http://localhost:1234/old/traces", //old
            ",,," }) //junk
    void resolveEndpoint(final String newUrl, final String newPath, final String legacyEndpoint, final String expected) {
        OtlpExporterRuntimeConfig config = getConfig(newUrl, newPath, legacyEndpoint);
        assertEquals(expected == null ? "" : expected, OtlpRecorder.resolveEndpoint(config));
    }

    private OtlpExporterRuntimeConfig getConfig(final String newUrl, final String newPath, final String legacyEndpoint) {
        return new OtlpExporterRuntimeConfig() {

            @Override
            public Optional<String> endpoint() {
                return Optional.ofNullable(newUrl);
            }

            @Override
            public OtlpExporterTracesConfig traces() {
                return new OtlpExporterTracesConfig() {
                    @Override
                    public Optional<String> endpoint() {
                        return Optional.ofNullable(newPath);
                    }

                    @Override
                    public Optional<String> legacyEndpoint() {
                        return Optional.ofNullable(legacyEndpoint);
                    }

                    @Override
                    public Map<String, String> headers() {
                        return null;
                    }

                    @Override
                    public Duration timeout() {
                        return null;
                    }

                    @Override
                    public Optional<byte[]> certificate() {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<ClientTlsConfig> client() {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<CompressionType> compression() {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<String> protocol() {
                        return Optional.empty();
                    }
                };
            }

            @Override
            public Optional<byte[]> certificate() {
                return Optional.empty();
            }

            @Override
            public Optional<ClientTlsConfig> client() {
                return Optional.empty();
            }

            @Override
            public Map<String, String> headers() {
                return null;
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
        };
    }
}
