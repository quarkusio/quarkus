package io.quarkus.oidc.common.runtime;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.net.ssl.SSLContext;

import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;

public interface OidcTlsSupport {

    TlsConfigSupport forConfig(OidcCommonConfig.Tls config);

    static OidcTlsSupport empty() {
        return new OidcTlsSupport() {
            @Override
            public TlsConfigSupport forConfig(OidcCommonConfig.Tls config) {
                return new TlsConfigSupport(Optional.empty(), null, false);
            }
        };
    }

    static OidcTlsSupport of(Supplier<TlsConfigurationRegistry> registrySupplier) {
        return of(registrySupplier.get());
    }

    static OidcTlsSupport of(TlsConfigurationRegistry registry) {
        return new OidcTlsSupport() {

            private final boolean globalTrustAll = registry.getDefault().map(new Function<TlsConfiguration, Boolean>() {
                @Override
                public Boolean apply(TlsConfiguration tlsConfiguration) {
                    return tlsConfiguration.isTrustAll();
                }
            }).orElse(Boolean.FALSE);

            @Override
            public TlsConfigSupport forConfig(OidcCommonConfig.Tls config) {
                return new TlsConfigSupport(config.tlsConfigurationName, registry, globalTrustAll);
            }

        };
    }

    final class TlsConfigSupport {

        private final TlsConfiguration tlsConfig;
        private final boolean globalTrustAll;
        private final String tlsConfigName;

        private TlsConfigSupport(Optional<String> tlsConfigurationName, TlsConfigurationRegistry registry,
                boolean globalTrustAll) {
            if (registry != null) {
                this.tlsConfig = TlsConfiguration.from(registry, tlsConfigurationName).orElse(null);
                this.tlsConfigName = tlsConfigurationName.orElse(null);
            } else {
                this.tlsConfig = null;
                this.tlsConfigName = null;
            }
            this.globalTrustAll = globalTrustAll;
        }

        public boolean useTlsRegistry() {
            return tlsConfig != null;
        }

        public TlsConfiguration getTlsConfig() {
            return tlsConfig;
        }

        public boolean isGlobalTrustAll() {
            return globalTrustAll;
        }

        public SSLContext getSslContext() {
            if (useTlsRegistry()) {
                try {
                    return tlsConfig.createSSLContext();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create SSLContext", e);
                }
            }
            return null;
        }

        public String getTlsConfigName() {
            return tlsConfigName;
        }
    }
}
