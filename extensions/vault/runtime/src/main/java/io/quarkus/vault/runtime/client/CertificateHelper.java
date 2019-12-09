package io.quarkus.vault.runtime.client;

import static javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class CertificateHelper {

    private static String CERT_BUNDLE_PATTERN = "-----BEGIN CERTIFICATE-----.*?-----END CERTIFICATE-----";
    private static Pattern certBundlePattern = Pattern.compile(CERT_BUNDLE_PATTERN, Pattern.DOTALL);

    public static TrustManager[] createTrustManagers(String cacert) throws GeneralSecurityException, IOException {

        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null);

        String certBundle = new String(Files.readAllBytes(Paths.get(cacert)), StandardCharsets.UTF_8);
        int start = 0;
        int count = 0;
        Matcher matcher = certBundlePattern.matcher(certBundle);

        while (matcher.find(start)) {
            ByteArrayInputStream inStream = new ByteArrayInputStream(matcher.group().getBytes(StandardCharsets.UTF_8));
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(inStream);
            keyStore.setCertificateEntry("cert_" + count++, certificate);
            start = matcher.end();
        }

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        return trustManagerFactory.getTrustManagers();
    }

    public static SSLContext createSslContext(TrustManager[] trustManagers) throws GeneralSecurityException {
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(new KeyManager[0], trustManagers, null);
        return sslContext;
    }

}
