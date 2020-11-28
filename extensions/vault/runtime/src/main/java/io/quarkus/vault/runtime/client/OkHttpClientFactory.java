package io.quarkus.vault.runtime.client;

import static io.quarkus.vault.runtime.client.CertificateHelper.createSslContext;
import static io.quarkus.vault.runtime.client.CertificateHelper.createTrustManagers;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.KUBERNETES;
import static io.quarkus.vault.runtime.config.VaultBootstrapConfig.KUBERNETES_CACERT;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Collections;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jboss.logging.Logger;

import io.quarkus.runtime.TlsConfig;
import io.quarkus.runtime.util.JavaVersionUtil;
import io.quarkus.vault.VaultException;
import io.quarkus.vault.runtime.config.VaultBootstrapConfig;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;

public class OkHttpClientFactory {

    private static final Logger log = Logger.getLogger(OkHttpClientFactory.class.getName());

    public static OkHttpClient createHttpClient(VaultBootstrapConfig vaultBootstrapConfig, TlsConfig tlsConfig) {

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(vaultBootstrapConfig.connectTimeout)
                .readTimeout(vaultBootstrapConfig.readTimeout);
        if (!JavaVersionUtil.isJava11OrHigher()) {
            // for Java versions lesser than Java 11, we explicitly use HTTP/1.1
            // to prevent HTTP/2 being included by default since there are known issues
            // with HTTP/2 protocol and ALPN negotiation over HTTPS in older Java versions.
            builder.protocols(Collections.singletonList(Protocol.HTTP_1_1));
        }

        try {
            boolean trustAll = vaultBootstrapConfig.tls.skipVerify.isPresent() ? vaultBootstrapConfig.tls.skipVerify.get()
                    : tlsConfig.trustAll;
            if (trustAll) {
                skipVerify(builder);
            } else if (vaultBootstrapConfig.tls.caCert.isPresent()) {
                cacert(builder, vaultBootstrapConfig.tls.caCert.get());
            } else if (vaultBootstrapConfig.getAuthenticationType() == KUBERNETES
                    && vaultBootstrapConfig.tls.useKubernetesCaCert) {
                cacert(builder, KUBERNETES_CACERT);
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new VaultException(e);
        }

        return builder.build();

    }

    private static void cacert(OkHttpClient.Builder builder, String cacert) throws GeneralSecurityException, IOException {
        log.debug("create SSLSocketFactory with tls " + cacert);
        sslSocketFactory(builder, createTrustManagers(cacert));
    }

    private static void skipVerify(OkHttpClient.Builder builder) throws GeneralSecurityException {
        log.debug("create SSLSocketFactory with tls.skip-verify");
        builder.hostnameVerifier((hostname, session) -> true);
        sslSocketFactory(builder, new TrustManager[] { new TrustAllTrustManager() });
    }

    private static void sslSocketFactory(OkHttpClient.Builder builder, TrustManager[] trustManagers)
            throws GeneralSecurityException {
        SSLContext sslContext = createSslContext(trustManagers);
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustManagers[0]);
    }

    static class TrustAllTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[] {};
        }
    }
}
