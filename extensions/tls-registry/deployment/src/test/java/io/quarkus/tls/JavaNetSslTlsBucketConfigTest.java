package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class JavaNetSslTlsBucketConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    TlsConfigurationRegistry certificates;

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
}
