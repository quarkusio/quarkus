package io.quarkus.amazon.common.runtime;

import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import software.amazon.awssdk.http.TlsTrustManagersProvider;

/**
 * A TlsTrustManagersProvider that creates a trustmanager trusting all certificates
 */
public class NoneTlsTrustManagersProvider implements TlsTrustManagersProvider {
    private static final NoneTlsTrustManagersProvider INSTANCE = new NoneTlsTrustManagersProvider();

    @Override
    public TrustManager[] trustManagers() {
        return new TrustManager[] { new InsecureTrustManager() };
    }

    private static class InsecureTrustManager implements X509TrustManager {
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

    public static NoneTlsTrustManagersProvider getInstance() {
        return INSTANCE;
    }
}
