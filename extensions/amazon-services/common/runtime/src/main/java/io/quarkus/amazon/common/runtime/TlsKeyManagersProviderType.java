package io.quarkus.amazon.common.runtime;

import software.amazon.awssdk.http.FileStoreTlsKeyManagersProvider;
import software.amazon.awssdk.http.SystemPropertyTlsKeyManagersProvider;
import software.amazon.awssdk.http.TlsKeyManagersProvider;

public enum TlsKeyManagersProviderType {
    NONE {
        @Override
        public TlsKeyManagersProvider create(TlsKeyManagersProviderConfig config) {
            return TlsKeyManagersProvider.noneProvider();
        }
    },
    SYSTEM_PROPERTY {
        @Override
        public TlsKeyManagersProvider create(TlsKeyManagersProviderConfig config) {
            return SystemPropertyTlsKeyManagersProvider.create();
        }
    },
    FILE_STORE {
        @Override
        public TlsKeyManagersProvider create(TlsKeyManagersProviderConfig config) {
            final FileStoreTlsManagersProviderConfig fileStore = config.fileStore;
            return FileStoreTlsKeyManagersProvider.create(fileStore.path.get(), fileStore.type.get(),
                    fileStore.password.orElse(null));
        }
    };

    public abstract TlsKeyManagersProvider create(TlsKeyManagersProviderConfig config);
}
