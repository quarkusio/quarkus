package io.quarkus.amazon.common.runtime;

import javax.net.ssl.TrustManager;

import software.amazon.awssdk.http.TlsTrustManagersProvider;

/**
 * A TlsTrustManagersProvider for loading system default truststore configuration
 */
public class SystemPropertyTlsTrustManagersProvider implements TlsTrustManagersProvider {
    private static final SystemPropertyTlsTrustManagersProvider INSTANCE = new SystemPropertyTlsTrustManagersProvider();

    @Override
    public TrustManager[] trustManagers() {
        return null;
    }

    public static SystemPropertyTlsTrustManagersProvider getInstance() {
        return INSTANCE;
    }
}
