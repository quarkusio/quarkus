package io.quarkus.tls.runtime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkus.tls.runtime.config.JKSTrustStoreConfig;
import io.quarkus.tls.runtime.config.KeyStoreConfig;
import io.quarkus.tls.runtime.config.P12TrustStoreConfig;
import io.quarkus.tls.runtime.config.PemCertsConfig;
import io.quarkus.tls.runtime.config.TlsBucketConfig;
import io.quarkus.tls.runtime.config.TrustStoreConfig;
import io.quarkus.tls.runtime.config.TrustStoreConfig.CertificateExpiryPolicy;
import io.quarkus.tls.runtime.config.TrustStoreCredentialProviderConfig;

/**
 * A {@link TlsBucketConfig} mimicking the way how SunJSSE locates the default truststore:
 * <ol>
 * <li>If the {@code javax.net.ssl.trustStore} property is defined, then it is honored
 * <li>If the {@code $JAVA_HOME/lib/security/jssecacerts} is a regular file, then it is used
 * <li>If the {@code $JAVA_HOME/lib/security/cacerts} is a regular file, then it is used
 * <li>Otherwise an {@link IllegalStateException} is thrown.
 * </ol>
 *
 * @since 3.18.0
 */
class JavaNetSslTlsBucketConfig implements TlsBucketConfig {

    private static final Logger log = Logger.getLogger(JavaNetSslTlsBucketConfig.class);

    JavaNetSslTlsBucketConfig() {
    }

    @Override
    public Optional<KeyStoreConfig> keyStore() {
        return Optional.empty();
    }

    @Override
    public Optional<TrustStoreConfig> trustStore() {
        final Path tsPath = defaultTrustStorePath();
        final Optional<JKSTrustStoreConfig> jksConfig;
        final Optional<P12TrustStoreConfig> p12Config;
        final String tsType = System.getProperty("javax.net.ssl.trustStoreType", KeyStore.getDefaultType())
                .toLowerCase(Locale.US);
        final Optional<String> password = Optional
                .ofNullable(System.getProperty("javax.net.ssl.trustStorePassword", "changeit"));
        switch (tsType) {
            case "pkcs12": {
                p12Config = Optional.of(new JavaNetSslStoreConfig(
                        tsPath,
                        password,
                        Optional.empty(),
                        null));
                jksConfig = Optional.empty();
                break;
            }
            case "jks": {
                p12Config = Optional.empty();
                jksConfig = Optional.of(new JavaNetSslStoreConfig(
                        tsPath,
                        password,
                        Optional.empty(),
                        null));
                break;
            }
            default:
                throw new IllegalArgumentException("Unexpected javax.net.ssl.trustStoreType: " + tsType);
        }
        final TrustStoreConfig tsCfg = new JavaNetSslTrustStoreConfig(p12Config, jksConfig, CertificateExpiryPolicy.WARN);
        return Optional.of(tsCfg);
    }

    static Path defaultTrustStorePath() {
        final String rawTsPath = System.getProperty("javax.net.ssl.trustStore");
        if (rawTsPath != null && !rawTsPath.isEmpty()) {
            log.debugf("Honoring javax.net.ssl.trustStore property value: %s", rawTsPath);
            return Path.of(rawTsPath);
        }
        final String javaHome = System.getProperty("java.home");
        if (javaHome == null || javaHome.isEmpty()) {
            throw new IllegalStateException(
                    "Could not locate the default Java truststore because the 'java.home' property is not set");
        }
        final Path javaHomePath = Path.of(javaHome);
        if (!Files.isDirectory(javaHomePath)) {
            throw new IllegalStateException("Could not locate the default Java truststore because the 'java.home' path '"
                    + javaHome + "' is not a directory");
        }
        final Path jssecacerts = javaHomePath.resolve("lib/security/jssecacerts");
        if (Files.isRegularFile(jssecacerts)) {
            log.debugf("Using %s as a truststore", jssecacerts);
            return jssecacerts;
        }
        final Path cacerts = javaHomePath.resolve("lib/security/cacerts");
        if (Files.isRegularFile(cacerts)) {
            log.debugf("Using %s as a truststore", cacerts);
            return cacerts;
        }
        throw new IllegalStateException(
                "Could not locate the default Java truststore. Tried javax.net.ssl.trustStore system property, " + jssecacerts
                        + " and " + cacerts);
    }

    @Override
    public Optional<List<String>> cipherSuites() {
        return Optional.empty();
    }

    @Override
    public Set<String> protocols() {
        return Set.of("TLSv1.3", "TLSv1.2");
    }

    @Override
    public Duration handshakeTimeout() {
        return Duration.ofSeconds(10L);
    }

    @Override
    public boolean alpn() {
        return true;
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

    static record JavaNetSslStoreConfig(Path path, Optional<String> password, Optional<String> alias,
            Optional<String> provider) implements P12TrustStoreConfig, JKSTrustStoreConfig {
    }

    static record JavaNetSslTrustStoreConfig(Optional<P12TrustStoreConfig> p12, Optional<JKSTrustStoreConfig> jks,
            CertificateExpiryPolicy certificateExpirationPolicy) implements TrustStoreConfig {

        @Override
        public Optional<PemCertsConfig> pem() {
            return Optional.empty();
        }

        @Override
        public TrustStoreCredentialProviderConfig credentialsProvider() {
            return null;
        }

    }
}