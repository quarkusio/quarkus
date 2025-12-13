package io.quarkus.tls.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.tls.runtime.config.KeyStoreConfig;
import io.quarkus.tls.runtime.config.TlsBucketConfig;
import io.quarkus.tls.runtime.config.TrustStoreConfig;

class VertxCertificateHolderTest {

    private VertxCertificateHolder holder;

    @BeforeEach
    void setUp() {
        // Create a minimal test config with only the required methods implemented
        TlsBucketConfig config = new TlsBucketConfig() {
            @Override
            public Optional<KeyStoreConfig> keyStore() {
                return Optional.empty();
            }

            @Override
            public Optional<TrustStoreConfig> trustStore() {
                return Optional.empty();
            }

            @Override
            public Optional<List<String>> cipherSuites() {
                return Optional.empty();
            }

            @Override
            public Set<String> protocols() {
                return Set.of();
            }

            @Override
            public Optional<List<Path>> certificateRevocationList() {
                return Optional.empty();
            }

            @Override
            public boolean trustAll() {
                return false;
            }

            @Override
            public Optional<String> hostnameVerificationAlgorithm() {
                return Optional.empty();
            }

            @Override
            public boolean alpn() {
                return false;
            }

            @Override
            public Duration handshakeTimeout() {
                return Duration.ofSeconds(10);
            }

            @Override
            public Optional<Duration> reloadPeriod() {
                return Optional.empty();
            }
        };
        holder = new VertxCertificateHolder(null, "test", config, null, null);
    }

    @Test
    void testDefault() {
        assertFalse(holder.warnIfOldProtocols(Set.of(TlsBucketConfig.DEFAULT_TLS_PROTOCOLS), "test"));
        assertFalse(holder.warnIfOldProtocols(Set.of(TlsBucketConfig.DEFAULT_TLS_PROTOCOLS.toLowerCase()), "test"));
    }

    @Test
    void testWarnIfOldProtocols_withSSL() {
        assertTrue(holder.warnIfOldProtocols(Set.of("SSLv3"), "test"));
        assertTrue(holder.warnIfOldProtocols(Set.of("SSLv2", "TLSv1.3"), "test"));
    }

    @Test
    void testWarnIfOldProtocols_withOldTLS() {
        assertTrue(holder.warnIfOldProtocols(Set.of("TLSv1"), "test"));
        assertTrue(holder.warnIfOldProtocols(Set.of("TLSv1.1"), "test"));
        assertTrue(holder.warnIfOldProtocols(Set.of("TLSv1", "TLSv1.1"), "test"));
    }

    @Test
    void testWarnIfOldProtocols_withTLSv12Only() {
        assertTrue(holder.warnIfOldProtocols(Set.of("TLSv1.2"), "test"));
    }

    @Test
    void testWarnIfOldProtocols_withTLSv13() {
        assertFalse(holder.warnIfOldProtocols(Set.of("TLSv1.3"), "test"));
    }

    @Test
    void testWarnIfOldProtocols_withTLSv12AndTLSv13() {
        assertFalse(holder.warnIfOldProtocols(Set.of("TLSv1.2", "TLSv1.3"), "test"));
    }

    @Test
    void testWarnIfOldProtocols_futureTLSVersions() {
        assertFalse(holder.warnIfOldProtocols(Set.of("TLSv1.4"), "test"));
        assertFalse(holder.warnIfOldProtocols(Set.of("TLSv1.5"), "test"));
        assertFalse(holder.warnIfOldProtocols(Set.of("TLSv1.10"), "test"));
        assertFalse(holder.warnIfOldProtocols(Set.of("TLSv2.0"), "test"));
        assertFalse(holder.warnIfOldProtocols(Set.of("TLSv3.0"), "test"));
    }

    @Test
    void testWarnIfOldProtocols_mixedOldAndModern() {
        assertTrue(holder.warnIfOldProtocols(Set.of("TLSv1.1", "TLSv1.3"), "test"));
        assertTrue(holder.warnIfOldProtocols(Set.of("SSLv3", "TLSv1.3"), "test"));
    }

    @Test
    void testWarnIfOldProtocols_caseInsensitive() {
        assertFalse(holder.warnIfOldProtocols(Set.of("tlsv1.3"), "test"));
        assertFalse(holder.warnIfOldProtocols(Set.of("TLSV1.3"), "test"));
        assertTrue(holder.warnIfOldProtocols(Set.of("sslv3"), "test"));
        assertTrue(holder.warnIfOldProtocols(Set.of("TLSV1"), "test"));
    }

    @Test
    void testWarnIfOldProtocols_withWhitespace() {
        assertFalse(holder.warnIfOldProtocols(Set.of(" TLSv1.3 "), "test"));
        assertTrue(holder.warnIfOldProtocols(Set.of(" TLSv1.2 "), "test"));
    }

    @Test
    void testWarnIfOldProtocols_emptySet() {
        assertTrue(holder.warnIfOldProtocols(Set.of(), "test"));
    }
}
