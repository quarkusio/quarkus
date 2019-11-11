package io.quarkus.dynamodb.runtime;

import software.amazon.awssdk.http.FileStoreTlsKeyManagersProvider;
import software.amazon.awssdk.http.SystemPropertyTlsKeyManagersProvider;
import software.amazon.awssdk.http.TlsKeyManagersProvider;

public enum TlsManagersProviderType {
    NONE {
        @Override
        public TlsKeyManagersProvider create(TlsManagersProviderConfig config) {
            return TlsKeyManagersProvider.noneProvider();
        }
    },
    SYSTEM_PROPERTY {
        @Override
        public TlsKeyManagersProvider create(TlsManagersProviderConfig config) {
            return SystemPropertyTlsKeyManagersProvider.create();
        }
    },
    FILE_STORE {
        @Override
        public TlsKeyManagersProvider create(TlsManagersProviderConfig config) {
            final TlsManagersProviderConfig.FileStoreTlsManagersProviderConfig fileStore = config.fileStore.get();
            return FileStoreTlsKeyManagersProvider.create(fileStore.path, fileStore.type,
                    fileStore.password);
        }
    };

    public abstract TlsKeyManagersProvider create(TlsManagersProviderConfig config);
}
