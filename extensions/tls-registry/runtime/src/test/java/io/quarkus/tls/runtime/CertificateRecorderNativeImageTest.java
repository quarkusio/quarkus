package io.quarkus.tls.runtime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.tls.runtime.config.KeyStoreConfig;
import io.quarkus.tls.runtime.config.PqcEnforcementPolicy;
import io.quarkus.tls.runtime.config.SslEngineType;
import io.quarkus.tls.runtime.config.TlsBucketConfig;
import io.quarkus.tls.runtime.config.TlsConfig;
import io.quarkus.tls.runtime.config.TrustStoreConfig;

/**
 * The OpenSSL engine (netty-tcnative) is disabled by the Quarkus GraalVM substitutions, so a TLS bucket that can
 * only be satisfied by it must be rejected early, with a message that names native mode as the cause, instead of
 * letting Vert.x fail at listen time with "OpenSSL is not available" / "neither JDK nor OpenSSL support it".
 */
class CertificateRecorderNativeImageTest {

    private static final boolean NATIVE = true;
    private static final boolean JVM = false;
    private static final boolean JDK_PQC = true;
    private static final boolean NO_JDK_PQC = false;
    private static final boolean OPENSSL = true;
    private static final boolean NO_OPENSSL = false;

    @Test
    void jvmModeNeverRejects() {
        assertThatCode(() -> CertificateRecorder.verifySslEngineSupportedInNativeImage(
                bucket(Optional.of(SslEngineType.OPENSSL), PqcEnforcementPolicy.STRICT), "foo", JVM, NO_JDK_PQC, NO_OPENSSL))
                .doesNotThrowAnyException();
    }

    @Test
    void nativeModeWithJdkEngineAndRelaxedPolicyIsAccepted() {
        assertThatCode(() -> CertificateRecorder.verifySslEngineSupportedInNativeImage(
                bucket(Optional.of(SslEngineType.JDKSSL), PqcEnforcementPolicy.RELAXED), "foo", NATIVE, NO_JDK_PQC, NO_OPENSSL))
                .doesNotThrowAnyException();
    }

    @Test
    void nativeModeWithEngineUnsetAndRelaxedPolicyIsAccepted() {
        assertThatCode(() -> CertificateRecorder.verifySslEngineSupportedInNativeImage(
                bucket(Optional.empty(), PqcEnforcementPolicy.RELAXED), "foo", NATIVE, NO_JDK_PQC, NO_OPENSSL))
                .doesNotThrowAnyException();
    }

    @Test
    void nativeModeWithOpenSslEngineIsRejectedNamingTheProperty() {
        assertThatThrownBy(() -> CertificateRecorder.verifySslEngineSupportedInNativeImage(
                bucket(Optional.of(SslEngineType.OPENSSL), PqcEnforcementPolicy.RELAXED), "foo", NATIVE, NO_JDK_PQC,
                NO_OPENSSL))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("quarkus.tls.foo.ssl-engine")
                .hasMessageContaining("native");
    }

    @Test
    void nativeModeWithOpenSslEngineOnDefaultBucketNamesTheUnnamedProperty() {
        assertThatThrownBy(() -> CertificateRecorder.verifySslEngineSupportedInNativeImage(
                bucket(Optional.of(SslEngineType.OPENSSL), PqcEnforcementPolicy.RELAXED), TlsConfig.DEFAULT_NAME,
                NATIVE, NO_JDK_PQC, NO_OPENSSL))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("quarkus.tls.ssl-engine")
                .satisfies(e -> org.assertj.core.api.Assertions.assertThat(e.getMessage())
                        .doesNotContain("quarkus.tls.<default>"));
    }

    @Test
    void nativeModeWithStrictPolicyAndNoJdkPqcIsRejectedNamingTheProperty() {
        assertThatThrownBy(() -> CertificateRecorder.verifySslEngineSupportedInNativeImage(
                bucket(Optional.empty(), PqcEnforcementPolicy.STRICT), "foo", NATIVE, NO_JDK_PQC, NO_OPENSSL))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("quarkus.tls.foo.pqc-enforcement-policy")
                .hasMessageContaining("strict")
                .hasMessageContaining("native");
    }

    @Test
    void nativeModeWithClientNegotiatedPolicyAndNoJdkPqcIsRejected() {
        assertThatThrownBy(() -> CertificateRecorder.verifySslEngineSupportedInNativeImage(
                bucket(Optional.empty(), PqcEnforcementPolicy.CLIENT_NEGOTIATED), "foo", NATIVE, NO_JDK_PQC, NO_OPENSSL))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("client-negotiated");
    }

    @Test
    void nativeModeWithStrictPolicyIsAcceptedWhenTheJdkEngineSupportsPqc() {
        // Once the JDK in use has JEP 527 and the Vert.x line in use probes for it, PQC works without OpenSSL.
        assertThatCode(() -> CertificateRecorder.verifySslEngineSupportedInNativeImage(
                bucket(Optional.empty(), PqcEnforcementPolicy.STRICT), "foo", NATIVE, JDK_PQC, NO_OPENSSL))
                .doesNotThrowAnyException();
    }

    @Test
    void nativeModeWithOpenSslEngineIsRejectedEvenWhenTheJdkEngineSupportsPqc() {
        assertThatThrownBy(() -> CertificateRecorder.verifySslEngineSupportedInNativeImage(
                bucket(Optional.of(SslEngineType.OPENSSL), PqcEnforcementPolicy.STRICT), "foo", NATIVE, JDK_PQC, NO_OPENSSL))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("quarkus.tls.foo.ssl-engine");
    }

    @Test
    void nativeModeWithOpenSslEngineIsAcceptedWhenTheOpenSslEngineIsActuallyAvailable() {
        // tcnative on the classpath: the substitutions are disabled and OpenSsl.isAvailable() reflects reality
        assertThatCode(() -> CertificateRecorder.verifySslEngineSupportedInNativeImage(
                bucket(Optional.of(SslEngineType.OPENSSL), PqcEnforcementPolicy.RELAXED), "foo", NATIVE, NO_JDK_PQC,
                OPENSSL))
                .doesNotThrowAnyException();
    }

    @Test
    void nativeModeWithStrictPolicyIsAcceptedWhenTheOpenSslEngineIsActuallyAvailable() {
        assertThatCode(() -> CertificateRecorder.verifySslEngineSupportedInNativeImage(
                bucket(Optional.empty(), PqcEnforcementPolicy.STRICT), "foo", NATIVE, NO_JDK_PQC, OPENSSL))
                .doesNotThrowAnyException();
    }

    private static TlsBucketConfig bucket(Optional<SslEngineType> engine, PqcEnforcementPolicy policy) {
        return new TlsBucketConfig() {
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
            public Duration handshakeTimeout() {
                return Duration.ofSeconds(10);
            }

            @Override
            public boolean alpn() {
                return false;
            }

            @Override
            public PqcEnforcementPolicy pqcEnforcementPolicy() {
                return policy;
            }

            @Override
            public Optional<List<String>> keyExchangeGroups() {
                return Optional.empty();
            }

            @Override
            public Optional<SslEngineType> sslEngine() {
                return engine;
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
        };
    }
}
