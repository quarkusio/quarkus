package io.quarkus.tls.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.naming.InvalidNameException;
import javax.net.ssl.TrustManagerFactory;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.tls.runtime.JavaxNetSslTrustStoreProvider.JavaNetSslTrustOptions;

public class JavaxNetSslTrustStoreProviderTest {

    @Test
    void copyCerts()
            throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException, InvalidNameException {
        final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore ks = KeyStore.getInstance("JKS");
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("JavaxNetSslTrustStoreProviderTest.jks")) {
            ks.load(in, "changeit".toCharArray());
        }
        Assertions.assertThat(ks.size()).isGreaterThan(0);
        tmf.init(ks);
        final KeyStore cacerts = JavaNetSslTrustOptions.copyCerts(tmf);
        Assertions.assertThat(cacerts.size()).isEqualTo(ks.size());
    }
}
