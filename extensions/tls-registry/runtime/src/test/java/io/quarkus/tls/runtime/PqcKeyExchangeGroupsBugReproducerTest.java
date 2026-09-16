package io.quarkus.tls.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import io.vertx.core.net.ServerSSLOptions;

/**
 * Reproducer for bug #56773: Key exchange groups are not ignored when PQC enforcement policy is RELAXED.
 *
 * This test demonstrates that when pqcEnforcementPolicy=RELAXED is used with key-exchange-groups,
 * the warning says groups will be ignored but they are actually still being set.
 */
class PqcKeyExchangeGroupsBugReproducerTest {

    @Test
    void testBugReproducer_RelaxedPolicyShouldIgnoreKeyExchangeGroups() {
        // GIVEN: Configuration with RELAXED policy and key exchange groups
        // This matches the bug report configuration:
        // quarkus.tls.pqc-enforcement-policy=relaxed
        // quarkus.tls.key-exchange-groups=X25519MLKEM768

        VertxCertificateHolder holder = createHolder(
                PqcEnforcementPolicy.RELAXED,
                List.of("X25519MLKEM768"));

        // WHEN: Getting SSL options
        ServerSSLOptions sslOptions = holder.getServerSSLOptions();

        // THEN: According to the warning message and PqcEnforcementPolicy.RELAXED documentation,
        // the key exchange groups should be IGNORED (null/not set)
        //
        // PqcEnforcementPolicy.RELAXED docs say:
        // "No PQC enforcement; standard TLS key exchange negotiation applies.
        //  The server neither advertises nor requires PQC groups."
        //
        // The warning says:
        // "The post-quantum groups will be ignored because 'relaxed' does not enforce post-quantum key exchange."

        // BEFORE FIX: This assertion would FAIL because groups were actually being set
        // AFTER FIX: This assertion should PASS because groups are now actually ignored
        assertNull(sslOptions.getKeyExchangeGroups(),
                "Key exchange groups should be null (ignored) when PQC enforcement policy is RELAXED");

        // Verify the policy itself is set correctly
        assertEquals(io.vertx.core.net.PqcEnforcementPolicy.RELAXED,
                sslOptions.getPqcEnforcementPolicy(),
                "PQC enforcement policy should be RELAXED");
    }

    @Test
    void testStrictPolicyShouldUseKeyExchangeGroups() {
        // GIVEN: Configuration with STRICT policy and key exchange groups
        VertxCertificateHolder holder = createHolder(
                PqcEnforcementPolicy.STRICT,
                List.of("X25519MLKEM768"));

        // WHEN: Getting SSL options
        ServerSSLOptions sslOptions = holder.getServerSSLOptions();

        // THEN: Key exchange groups SHOULD be set
        assertNotNull(sslOptions.getKeyExchangeGroups(),
                "Key exchange groups should be set when PQC enforcement policy is STRICT");
        assertEquals(1, sslOptions.getKeyExchangeGroups().size());
        assertTrue(sslOptions.getKeyExchangeGroups().contains("X25519MLKEM768"));
        assertEquals(io.vertx.core.net.PqcEnforcementPolicy.STRICT,
                sslOptions.getPqcEnforcementPolicy());
    }

    @Test
    void testClientNegotiatedPolicyShouldUseKeyExchangeGroups() {
        // GIVEN: Configuration with CLIENT_NEGOTIATED policy and key exchange groups
        VertxCertificateHolder holder = createHolder(
                PqcEnforcementPolicy.CLIENT_NEGOTIATED,
                List.of("X25519MLKEM768", "SecP256r1MLKEM768"));

        // WHEN: Getting SSL options
        ServerSSLOptions sslOptions = holder.getServerSSLOptions();

        // THEN: Key exchange groups SHOULD be set
        assertNotNull(sslOptions.getKeyExchangeGroups(),
                "Key exchange groups should be set when PQC enforcement policy is CLIENT_NEGOTIATED");
        assertEquals(2, sslOptions.getKeyExchangeGroups().size());
        assertTrue(sslOptions.getKeyExchangeGroups().contains("X25519MLKEM768"));
        assertTrue(sslOptions.getKeyExchangeGroups().contains("SecP256r1MLKEM768"));
        assertEquals(io.vertx.core.net.PqcEnforcementPolicy.CLIENT_NEGOTIATED,
                sslOptions.getPqcEnforcementPolicy());
    }

    @Test
    void testRelaxedPolicyWithoutGroupsShouldWorkNormally() {
        // GIVEN: Configuration with RELAXED policy and NO key exchange groups
        VertxCertificateHolder holder = createHolder(
                PqcEnforcementPolicy.RELAXED,
                null);

        // WHEN: Getting SSL options
        ServerSSLOptions sslOptions = holder.getServerSSLOptions();

        // THEN: No groups should be set and no warning should be issued
        assertNull(sslOptions.getKeyExchangeGroups(),
                "Key exchange groups should be null when not configured");
        assertEquals(io.vertx.core.net.PqcEnforcementPolicy.RELAXED,
                sslOptions.getPqcEnforcementPolicy());
    }

    @Test
    void testEdgeCase_EmptyKeyExchangeGroupsList() {
        // GIVEN: Configuration with STRICT policy and empty list of groups
        VertxCertificateHolder holder = createHolder(
                PqcEnforcementPolicy.STRICT,
                List.of());

        // WHEN: Getting SSL options
        ServerSSLOptions sslOptions = holder.getServerSSLOptions();

        // THEN: Empty list should be set
        assertNotNull(sslOptions.getKeyExchangeGroups());
        assertEquals(0, sslOptions.getKeyExchangeGroups().size());
    }

    @Test
    void testEdgeCase_MultipleKeyExchangeGroupsWithRelaxed() {
        // GIVEN: Multiple groups with RELAXED policy
        VertxCertificateHolder holder = createHolder(
                PqcEnforcementPolicy.RELAXED,
                List.of("X25519MLKEM768", "SecP256r1MLKEM768", "SecP384r1MLKEM1024"));

        // WHEN: Getting SSL options
        ServerSSLOptions sslOptions = holder.getServerSSLOptions();

        // THEN: ALL groups should be ignored
        assertNull(sslOptions.getKeyExchangeGroups(),
                "All key exchange groups should be ignored when PQC enforcement policy is RELAXED");
    }

    // Helper method to create a VertxCertificateHolder with specific configuration
    private VertxCertificateHolder createHolder(PqcEnforcementPolicy policy, List<String> keyExchangeGroups) {
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
                return Set.of("TLSv1.3");
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
                return true;
            }

            @Override
            public PqcEnforcementPolicy pqcEnforcementPolicy() {
                return policy;
            }

            @Override
            public Optional<List<String>> keyExchangeGroups() {
                return keyExchangeGroups != null ? Optional.of(keyExchangeGroups) : Optional.empty();
            }

            @Override
            public Duration handshakeTimeout() {
                return Duration.ofSeconds(10);
            }

            @Override
            public Optional<Duration> reloadPeriod() {
                return Optional.empty();
            }

            @Override
            public Optional<SslEngineType> sslEngine() {
                return Optional.empty();
            }
        };

        return new VertxCertificateHolder(null, "test-bucket", config, null, null);
    }
}
