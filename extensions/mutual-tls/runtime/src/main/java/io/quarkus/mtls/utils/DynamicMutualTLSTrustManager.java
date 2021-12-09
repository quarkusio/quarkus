package io.quarkus.mtls.utils;

import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;

import io.quarkus.mtls.MutualTLSConfig;
import io.quarkus.mtls.MutualTLSProvider;

/**
 * Trust manager implementation that pulls from a mutual TLS provider configuration and
 * refreshes automatically as certificates expire and are reissued.
 */
public class DynamicMutualTLSTrustManager extends X509ExtendedTrustManager {

    private final MutualTLSProvider mutualTLSProvider;
    private final String mutualTLSProviderName;
    private MutualTLSConfig lastConfig;
    private X509ExtendedTrustManager trustManagerDelegate;

    public DynamicMutualTLSTrustManager(MutualTLSProvider mutualTLSProvider, String mutualTLSProviderName) {
        super();
        this.mutualTLSProvider = mutualTLSProvider;
        this.mutualTLSProviderName = mutualTLSProviderName;
    }

    private MutualTLSConfig getConfig() {
        return mutualTLSProvider.getConfig(mutualTLSProviderName);
    }

    private X509ExtendedTrustManager getDelegate() {
        MutualTLSConfig config = getConfig();
        if (config != lastConfig) {
            // Update cached trust manager
            try {
                KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                ks.load(null);
                for (X509Certificate cert : config.getTrustedCertificates()) {
                    ks.setCertificateEntry(cert.getSubjectX500Principal().toString(), cert);
                }

                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ks);

                for (TrustManager tm : tmf.getTrustManagers()) {
                    if (tm instanceof X509ExtendedTrustManager) {
                        trustManagerDelegate = (X509ExtendedTrustManager) tm;
                        break;
                    }
                }
                if (trustManagerDelegate == null) {
                    throw new GeneralSecurityException("No X509TrustManager found");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            lastConfig = config;
        }
        return trustManagerDelegate;
    }

    /// X509TrustManager

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        getDelegate().checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        getDelegate().checkServerTrusted(chain, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return getDelegate().getAcceptedIssuers();
    }

    /// X509ExtendedTrustManager

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
            throws CertificateException {
        getDelegate().checkClientTrusted(chain, authType, socket);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
            throws CertificateException {
        getDelegate().checkServerTrusted(chain, authType, socket);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
            throws CertificateException {
        getDelegate().checkClientTrusted(chain, authType, engine);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
            throws CertificateException {
        getDelegate().checkServerTrusted(chain, authType, engine);
    }
}
