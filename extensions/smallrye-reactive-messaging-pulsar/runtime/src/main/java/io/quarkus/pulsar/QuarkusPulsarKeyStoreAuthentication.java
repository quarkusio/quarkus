package io.quarkus.pulsar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.pulsar.client.api.Authentication;
import org.apache.pulsar.client.api.AuthenticationDataProvider;
import org.apache.pulsar.client.api.PulsarClientException;

import io.quarkus.tls.TlsConfiguration;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.KeyStoreOptionsBase;

public class QuarkusPulsarKeyStoreAuthentication implements Authentication {
    public static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    public static final String END_CERT = "-----END CERTIFICATE-----";
    public static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");

    private final TlsConfiguration configuration;

    public QuarkusPulsarKeyStoreAuthentication(TlsConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String getAuthMethodName() {
        return "quarkus-pulsar";
    }

    @Override
    public void configure(Map<String, String> authParams) {

    }

    @Override
    public void start() throws PulsarClientException {

    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public AuthenticationDataProvider getAuthData() throws PulsarClientException {
        try {
            // Extract keystore password
            String keyStorePwd = ((KeyStoreOptionsBase) configuration.getKeyStoreOptions()).getPassword();
            // Certificates
            KeyStore keyStore = configuration.getKeyStore();
            String alias = keyStore.aliases().nextElement();
            // TODO do we need to handle certificate chains here?
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
            X509Certificate[] certificates = Stream.of(cert).toArray(X509Certificate[]::new);

            // Private key
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias,
                    new KeyStore.PasswordProtection(keyStorePwd.toCharArray()));
            PrivateKey privateKey = privateKeyEntry.getPrivateKey();

            // Trust store certificate
            KeyStore trustStore = configuration.getTrustStore();
            String trustAlias = trustStore.aliases().nextElement();
            Certificate trustStoreCertificate = trustStore.getCertificate(trustAlias);
            Buffer formatCrtFileContents = formatCrtFileContents(trustStoreCertificate);

            return new QuarkusPulsarAuthenticationData(certificates, privateKey, formatCrtFileContents);
        } catch (Exception e) {
            throw new PulsarClientException(e);
        }
    }

    public static Buffer formatCrtFileContents(Certificate certificate) throws CertificateEncodingException {
        Buffer buffer = Buffer.buffer();
        final Base64.Encoder encoder = Base64.getMimeEncoder(64, LINE_SEPARATOR.getBytes());

        buffer.appendString(BEGIN_CERT)
                .appendString(LINE_SEPARATOR)
                .appendBytes(encoder.encode(certificate.getEncoded()))
                .appendString(LINE_SEPARATOR)
                .appendString(END_CERT);
        return buffer;
    }

    private static class QuarkusPulsarAuthenticationData implements AuthenticationDataProvider {

        private final Certificate[] certs;
        private final PrivateKey privateKey;
        private final InputStream trustStoreStream;

        public QuarkusPulsarAuthenticationData(Certificate[] certs, PrivateKey privateKey, Buffer trustStore) {
            this.certs = certs;
            this.privateKey = privateKey;
            this.trustStoreStream = new ByteArrayInputStream(trustStore.getBytes());
        }

        public boolean hasDataForTls() {
            return true;
        }

        @Override
        public Certificate[] getTlsCertificates() {
            return certs;
        }

        @Override
        public PrivateKey getTlsPrivateKey() {
            return privateKey;
        }

        @Override
        public InputStream getTlsTrustStoreStream() {
            return trustStoreStream;
        }

    }
}
