package io.quarkus.tls.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.tls.runtime.config.KeyStoreConfig;
import io.quarkus.tls.runtime.config.PqcEnforcementPolicy;
import io.quarkus.tls.runtime.config.SslEngineType;
import io.quarkus.tls.runtime.config.TlsBucketConfig;
import io.quarkus.tls.runtime.config.TrustStoreConfig;
import io.vertx.core.Vertx;
import io.vertx.core.net.ServerSSLOptions;

/**
 * Test for issue Quarkusio-56774: When STRICT PQC enforcement policy is set with mixed PQC and non-PQC
 * key exchange groups (e.g., X25519MLKEM768,X25519), the non-PQC groups should be filtered out.
 */
public class PqcKeyExchangeGroupFilteringTest {

    @Test
    void testStrictPolicyFiltersOutNonPqcGroups() {
        TlsBucketConfig config = new TestTlsBucketConfig(
                PqcEnforcementPolicy.STRICT,
                List.of("X25519MLKEM768", "X25519") // Mixed PQC and classical
        );

        Vertx vertx = Vertx.vertx();
        try {
            VertxCertificateHolder holder = new VertxCertificateHolder(vertx, "test", config, null, null);
            ServerSSLOptions sslOptions = holder.getServerSSLOptions();

            // With STRICT policy, X25519 should be filtered out, only X25519MLKEM768 should remain
            assertThat(sslOptions.getKeyExchangeGroups())
                    .containsExactly("X25519MLKEM768")
                    .doesNotContain("X25519");
        } finally {
            vertx.close();
        }
    }

    @Test
    void testStrictPolicyWithOnlyPqcGroupsKeepsAll() {
        TlsBucketConfig config = new TestTlsBucketConfig(
                PqcEnforcementPolicy.STRICT,
                List.of("X25519MLKEM768", "SecP256r1MLKEM768") // All PQC
        );

        Vertx vertx = Vertx.vertx();
        try {
            VertxCertificateHolder holder = new VertxCertificateHolder(vertx, "test", config, null, null);
            ServerSSLOptions sslOptions = holder.getServerSSLOptions();

            // All PQC groups should be kept
            assertThat(sslOptions.getKeyExchangeGroups())
                    .containsExactlyInAnyOrder("X25519MLKEM768", "SecP256r1MLKEM768");
        } finally {
            vertx.close();
        }
    }

    @Test
    void testStrictPolicyWithOnlyNonPqcGroupsRemovesAll() {
        TlsBucketConfig config = new TestTlsBucketConfig(
                PqcEnforcementPolicy.STRICT,
                List.of("X25519", "P-256") // Only classical
        );

        Vertx vertx = Vertx.vertx();
        try {
            VertxCertificateHolder holder = new VertxCertificateHolder(vertx, "test", config, null, null);
            ServerSSLOptions sslOptions = holder.getServerSSLOptions();

            // All non-PQC groups should be filtered out, list should be null/empty
            // (Vert.x will use its PQC defaults)
            assertThat(sslOptions.getKeyExchangeGroups()).isNullOrEmpty();
        } finally {
            vertx.close();
        }
    }

    @Test
    void testClientNegotiatedPolicyDoesNotFilter() {
        TlsBucketConfig config = new TestTlsBucketConfig(
                PqcEnforcementPolicy.CLIENT_NEGOTIATED,
                List.of("X25519MLKEM768", "X25519") // Mixed PQC and classical
        );

        Vertx vertx = Vertx.vertx();
        try {
            VertxCertificateHolder holder = new VertxCertificateHolder(vertx, "test", config, null, null);
            ServerSSLOptions sslOptions = holder.getServerSSLOptions();

            // CLIENT_NEGOTIATED policy should keep all groups as-is
            assertThat(sslOptions.getKeyExchangeGroups())
                    .containsExactlyInAnyOrder("X25519MLKEM768", "X25519");
        } finally {
            vertx.close();
        }
    }

    @Test
    void testRelaxedPolicyDoesNotFilter() {
        TlsBucketConfig config = new TestTlsBucketConfig(
                PqcEnforcementPolicy.RELAXED,
                List.of("X25519MLKEM768", "X25519") // Mixed PQC and classical
        );

        Vertx vertx = Vertx.vertx();
        try {
            VertxCertificateHolder holder = new VertxCertificateHolder(vertx, "test", config, null, null);
            ServerSSLOptions sslOptions = holder.getServerSSLOptions();

            // RELAXED policy should keep all groups as-is (though they will be ignored)
            assertThat(sslOptions.getKeyExchangeGroups())
                    .containsExactlyInAnyOrder("X25519MLKEM768", "X25519");
        } finally {
            vertx.close();
        }
    }

    @Test
    void testStrictPolicyWithAllPqcGroups() {
        TlsBucketConfig config = new TestTlsBucketConfig(
                PqcEnforcementPolicy.STRICT,
                List.of("X25519MLKEM768", "SecP256r1MLKEM768", "SecP384r1MLKEM1024") // All PQC
        );

        Vertx vertx = Vertx.vertx();
        try {
            VertxCertificateHolder holder = new VertxCertificateHolder(vertx, "test", config, null, null);
            ServerSSLOptions sslOptions = holder.getServerSSLOptions();

            // All PQC groups should be preserved
            assertThat(sslOptions.getKeyExchangeGroups())
                    .containsExactlyInAnyOrder("X25519MLKEM768", "SecP256r1MLKEM768", "SecP384r1MLKEM1024");
        } finally {
            vertx.close();
        }
    }

    @Test
    void testStrictPolicyWithMixedAllGroupTypes() {
        TlsBucketConfig config = new TestTlsBucketConfig(
                PqcEnforcementPolicy.STRICT,
                List.of("X25519MLKEM768", "SecP256r1MLKEM768", "X25519", "P-256") // Mixed
        );

        Vertx vertx = Vertx.vertx();
        try {
            VertxCertificateHolder holder = new VertxCertificateHolder(vertx, "test", config, null, null);
            ServerSSLOptions sslOptions = holder.getServerSSLOptions();

            // Only PQC groups should remain, classical filtered out
            assertThat(sslOptions.getKeyExchangeGroups())
                    .containsExactlyInAnyOrder("X25519MLKEM768", "SecP256r1MLKEM768")
                    .doesNotContain("X25519", "P-256");
        } finally {
            vertx.close();
        }
    }

    @Test
    void testCaseSensitivityPqcGroups() {
        TlsBucketConfig config = new TestTlsBucketConfig(
                PqcEnforcementPolicy.STRICT,
                List.of("x25519mlkem768", "SECP256R1MLKEM768", "X25519") // Mixed case
        );

        Vertx vertx = Vertx.vertx();
        try {
            VertxCertificateHolder holder = new VertxCertificateHolder(vertx, "test", config, null, null);
            ServerSSLOptions sslOptions = holder.getServerSSLOptions();

            // Case-insensitive matching, only PQC groups preserved
            assertThat(sslOptions.getKeyExchangeGroups())
                    .containsExactlyInAnyOrder("x25519mlkem768", "SECP256R1MLKEM768")
                    .doesNotContain("X25519");
        } finally {
            vertx.close();
        }
    }

    @Test
    void testStrictPolicyWithDuplicatePqcGroups() {
        TlsBucketConfig config = new TestTlsBucketConfig(
                PqcEnforcementPolicy.STRICT,
                List.of("X25519MLKEM768", "X25519MLKEM768", "X25519") // Duplicates
        );

        Vertx vertx = Vertx.vertx();
        try {
            VertxCertificateHolder holder = new VertxCertificateHolder(vertx, "test", config, null, null);
            ServerSSLOptions sslOptions = holder.getServerSSLOptions();

            // Duplicates are preserved (filtering doesn't deduplicate), classical removed
            assertThat(sslOptions.getKeyExchangeGroups())
                    .hasSize(2)
                    .allMatch(g -> g.equals("X25519MLKEM768"));
        } finally {
            vertx.close();
        }
    }

    @Test
    void testClientNegotiatedWithOnlyPqcGroups() {
        TlsBucketConfig config = new TestTlsBucketConfig(
                PqcEnforcementPolicy.CLIENT_NEGOTIATED,
                List.of("X25519MLKEM768", "SecP256r1MLKEM768") // Only PQC
        );

        Vertx vertx = Vertx.vertx();
        try {
            VertxCertificateHolder holder = new VertxCertificateHolder(vertx, "test", config, null, null);
            ServerSSLOptions sslOptions = holder.getServerSSLOptions();

            // CLIENT_NEGOTIATED doesn't filter, keeps all as-is
            assertThat(sslOptions.getKeyExchangeGroups())
                    .containsExactlyInAnyOrder("X25519MLKEM768", "SecP256r1MLKEM768");
        } finally {
            vertx.close();
        }
    }

    @Test
    void testClientNegotiatedWithOnlyClassicalGroups() {
        TlsBucketConfig config = new TestTlsBucketConfig(
                PqcEnforcementPolicy.CLIENT_NEGOTIATED,
                List.of("X25519", "P-256") // Only classical
        );

        Vertx vertx = Vertx.vertx();
        try {
            VertxCertificateHolder holder = new VertxCertificateHolder(vertx, "test", config, null, null);
            ServerSSLOptions sslOptions = holder.getServerSSLOptions();

            // CLIENT_NEGOTIATED doesn't filter, even classical-only groups
            assertThat(sslOptions.getKeyExchangeGroups())
                    .containsExactlyInAnyOrder("X25519", "P-256");
        } finally {
            vertx.close();
        }
    }

    @Test
    void testStrictPolicyWithWhitespaceTrimming() {
        TlsBucketConfig config = new TestTlsBucketConfig(
                PqcEnforcementPolicy.STRICT,
                List.of(" X25519MLKEM768 ", "  X25519  ") // With whitespace
        );

        Vertx vertx = Vertx.vertx();
        try {
            VertxCertificateHolder holder = new VertxCertificateHolder(vertx, "test", config, null, null);
            ServerSSLOptions sslOptions = holder.getServerSSLOptions();

            // Groups are preserved as-is (Vert.x handles trimming), but filtering works
            // The filtering logic trims for comparison, so X25519 is filtered out
            assertThat(sslOptions.getKeyExchangeGroups())
                    .hasSize(1)
                    .anyMatch(g -> g.trim().equalsIgnoreCase("X25519MLKEM768"));
        } finally {
            vertx.close();
        }
    }

    private static class TestTlsBucketConfig implements TlsBucketConfig {
        private final PqcEnforcementPolicy pqcPolicy;
        private final List<String> keyExchangeGroups;

        public TestTlsBucketConfig(PqcEnforcementPolicy pqcPolicy, List<String> keyExchangeGroups) {
            this.pqcPolicy = pqcPolicy;
            this.keyExchangeGroups = keyExchangeGroups;
        }

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
            return Set.of("TLSv1.3");
        }

        @Override
        public Duration handshakeTimeout() {
            return Duration.ofSeconds(10);
        }

        @Override
        public boolean alpn() {
            return true;
        }

        @Override
        public PqcEnforcementPolicy pqcEnforcementPolicy() {
            return pqcPolicy;
        }

        @Override
        public Optional<List<String>> keyExchangeGroups() {
            return Optional.of(keyExchangeGroups);
        }

        @Override
        public Optional<SslEngineType> sslEngine() {
            return Optional.empty();
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
        public Optional<Duration> reloadPeriod() {
            return Optional.empty();
        }
    }
}
