package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.Vertx;

public class JavaxNetSslTrustStoreProviderTest {
    @RegisterExtension
    static final QuarkusUnitTest config = createConfig();

    static QuarkusUnitTest createConfig() {
        final Path tsPath = defaultTrustStorePath();
        String tsType = System.getProperty("javax.net.ssl.trustStoreType", KeyStore.getDefaultType())
                .toLowerCase(Locale.US);
        if (tsType.equals("pkcs12")) {
            tsType = "p12";
        }
        final String password = System.getProperty("javax.net.ssl.trustStorePassword", "changeit");

        return new QuarkusUnitTest()
                .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
                .overrideConfigKey("quarkus.tls.javaNetSslLike.trust-store." + tsType + ".path", tsPath.toString())
                .overrideConfigKey("quarkus.tls.javaNetSslLike.trust-store." + tsType + ".password", password);
    }

    static Path defaultTrustStorePath() {
        final String rawTsPath = System.getProperty("javax.net.ssl.trustStore");
        if (rawTsPath != null && !rawTsPath.isEmpty()) {
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
            return jssecacerts;
        }
        final Path cacerts = javaHomePath.resolve("lib/security/cacerts");
        if (Files.isRegularFile(cacerts)) {
            return cacerts;
        }
        throw new IllegalStateException(
                "Could not locate the default Java truststore. Tried javax.net.ssl.trustStore system property, " + jssecacerts
                        + " and " + cacerts);
    }

    @Inject
    TlsConfigurationRegistry certificates;

    @Inject
    Vertx vertx;

    @Test
    void test() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        TlsConfiguration def = certificates.get("javax.net.ssl").orElseThrow();

        assertThat(def.getTrustStoreOptions()).isNotNull();
        final KeyStore actualTs = def.getTrustStore();
        assertThat(actualTs).isNotNull();

        /*
         * Get the default trust managers, one of which should be SunJSSE based,
         * which in turn should use the same default trust store lookup algo
         * like we do in io.quarkus.tls.runtime.JavaNetSslTlsBucketConfig.defaultTrustStorePath()
         */
        final TrustManagerFactory trustManagerFactory = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);
        final List<X509TrustManager> defaultTrustManagers = Stream.of(trustManagerFactory.getTrustManagers())
                .filter(m -> m instanceof X509TrustManager)
                .map(m -> (X509TrustManager) m)
                .collect(Collectors.toList());
        assertThat(defaultTrustManagers).hasSizeGreaterThan(0);

        final List<String> actualAliases = Collections.list(actualTs.aliases());
        assertThat(actualAliases).hasSizeGreaterThan(0);

        for (String alias : actualAliases) {
            /*
             * Get the certs from the trust store loaded by us from $JAVA_HOME/lib/security/cacerts or similar
             * and validate those against the default trust managers.
             * In that way we make sure indirectly that we have loaded some valid trust material.
             */
            final X509Certificate cert = (X509Certificate) actualTs.getCertificate(alias);
            CertificateException lastException = null;
            boolean passed = false;
            for (X509TrustManager tm : defaultTrustManagers) {
                try {
                    tm.checkServerTrusted(new X509Certificate[] { cert }, "RSA");
                    passed = true;
                    break;
                } catch (CertificateException e) {
                    lastException = e;
                }
            }
            if (!passed && lastException != null) {
                throw lastException;
            }
        }

    }

    @Test
    void certs() throws Exception {
        /*
         * The javaNetSslLike named TLS bucket mimics what JavaNetSslTrustStoreProvider does programmatically.
         * By asserting that the set of certs they contain are equal, we make sure that JavaNetSslTrustStoreProvider
         * behaves correctly.
         */
        final TrustManager[] javaNetSslTrustManagers = trustManagers("javax.net.ssl");
        final TrustManager[] javaNetSslLikeTrustManagers = trustManagers("javaNetSslLike");
        assertThat(javaNetSslTrustManagers.length).isEqualTo(javaNetSslLikeTrustManagers.length);
        for (int i = 0; i < javaNetSslTrustManagers.length; i++) {
            X509TrustManager javaNetSslTm = (X509TrustManager) javaNetSslTrustManagers[i];
            X509TrustManager javaNetSslLikeTm = (X509TrustManager) javaNetSslLikeTrustManagers[i];
            assertThat(javaNetSslTm.getAcceptedIssuers().length).isGreaterThan(0);
            assertThat(javaNetSslTm.getAcceptedIssuers()).containsExactlyInAnyOrder(javaNetSslLikeTm.getAcceptedIssuers());
        }
    }

    TrustManager[] trustManagers(String key) throws Exception {
        final TlsConfiguration javaNetSsl = certificates.get(key).orElseThrow();
        final TrustManagerFactory javaNetSslTrustManagerFactory = javaNetSsl.getSSLOptions().getTrustOptions()
                .getTrustManagerFactory(vertx);
        return javaNetSslTrustManagerFactory.getTrustManagers();
    }
}
