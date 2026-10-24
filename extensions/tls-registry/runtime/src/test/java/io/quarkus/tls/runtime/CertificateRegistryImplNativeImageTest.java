package io.quarkus.tls.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.tls.runtime.config.SslEngineType;
import io.quarkus.tls.runtime.config.TlsBucketConfig;
import io.quarkus.tls.runtime.config.TlsConfig;

/**
 * The OpenSSL engine (netty-tcnative) is disabled by the Quarkus GraalVM substitutions, so a TLS bucket that requests
 * it must be rejected early, with a message that names native mode as the cause, instead of letting Vert.x fail at
 * listen time with "OpenSSL is not available".
 */
class CertificateRegistryImplNativeImageTest {

    private static final boolean NATIVE = true;
    private static final boolean JVM = false;

    @Test
    void jvmModeNeverRejects() {
        assertThatCode(() -> CertificateRegistryImpl.verifySslEngineSupportedInNativeImage(
                bucket(Optional.of(SslEngineType.OPENSSL)), "foo", JVM))
                .doesNotThrowAnyException();
    }

    @Test
    void nativeModeWithJdkEngineIsAccepted() {
        assertThatCode(() -> CertificateRegistryImpl.verifySslEngineSupportedInNativeImage(
                bucket(Optional.of(SslEngineType.JDKSSL)), "foo", NATIVE))
                .doesNotThrowAnyException();
    }

    @Test
    void nativeModeWithEngineUnsetIsAccepted() {
        // the engine Vert.x selects for the policy, and whether it supports it, is validated by Vert.x itself
        assertThatCode(() -> CertificateRegistryImpl.verifySslEngineSupportedInNativeImage(
                bucket(Optional.empty()), "foo", NATIVE))
                .doesNotThrowAnyException();
    }

    @Test
    void nativeModeWithOpenSslEngineIsRejectedNamingTheProperty() {
        assertThatThrownBy(() -> CertificateRegistryImpl.verifySslEngineSupportedInNativeImage(
                bucket(Optional.of(SslEngineType.OPENSSL)), "foo", NATIVE))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("quarkus.tls.foo.ssl-engine")
                .hasMessageContaining("native");
    }

    @Test
    void nativeModeWithOpenSslEngineOnDefaultBucketNamesTheUnnamedProperty() {
        assertThatThrownBy(() -> CertificateRegistryImpl.verifySslEngineSupportedInNativeImage(
                bucket(Optional.of(SslEngineType.OPENSSL)), TlsConfig.DEFAULT_NAME,
                NATIVE))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("quarkus.tls.ssl-engine")
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain("quarkus.tls.<default>"));
    }

    private static TlsBucketConfig bucket(Optional<SslEngineType> engine) {
        TlsBucketConfig config = mock(TlsBucketConfig.class);
        when(config.sslEngine()).thenReturn(engine);
        return config;
    }
}
