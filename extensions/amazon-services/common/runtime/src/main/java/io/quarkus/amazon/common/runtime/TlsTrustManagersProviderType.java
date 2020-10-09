package io.quarkus.amazon.common.runtime;

import software.amazon.awssdk.http.TlsTrustManagersProvider;

public enum TlsTrustManagersProviderType {
    TRUST_ALL {
        @Override
        public TlsTrustManagersProvider create(TlsTrustManagersProviderConfig config) {
            return new NoneTlsTrustManagersProvider();
        }
    },
    SYSTEM_PROPERTY {
        @Override
        public TlsTrustManagersProvider create(TlsTrustManagersProviderConfig config) {
            return new SystemPropertyTlsTrustManagersProvider();
        }
    },
    FILE_STORE {
        @Override
        public TlsTrustManagersProvider create(TlsTrustManagersProviderConfig config) {
            return new FileStoreTlsTrustManagersProvider(config.fileStore);
        }
    };

    public abstract TlsTrustManagersProvider create(TlsTrustManagersProviderConfig config);
}
