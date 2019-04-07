package io.quarkus.vault.runtime.client;

import static io.quarkus.vault.runtime.client.CertificateHelper.createSslContext;
import static io.quarkus.vault.runtime.client.CertificateHelper.createTrustManagers;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.KUBERNETES;
import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.KUBERNETES_CACERT;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jboss.logging.Logger;

import io.quarkus.vault.VaultException;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;
import okhttp3.OkHttpClient;

public class OkHttpClientFactory {

    private static final Logger log = Logger.getLogger(OkHttpClientFactory.class.getName());

    public static OkHttpClient createHttpClient(VaultRuntimeConfig serverConfig) {

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(serverConfig.connectTimeout)
                .readTimeout(serverConfig.readTimeout);

        try {
            if (serverConfig.tls.skipVerify) {
                skipVerify(builder);
            } else if (serverConfig.tls.caCert.isPresent()) {
                cacert(builder, serverConfig.tls.caCert.get());
            } else if (serverConfig.getAuthenticationType() == KUBERNETES && serverConfig.tls.useKubernetesCaCert) {
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
