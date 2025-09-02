package io.quarkus.tls.runtime;

import java.security.KeyStoreException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Default;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.tls.runtime.config.TlsBucketConfig;
import io.quarkus.tls.runtime.config.TlsConfig;
import io.quarkus.tls.runtime.keystores.JKSKeyStores;
import io.quarkus.tls.runtime.keystores.P12KeyStores;
import io.quarkus.tls.runtime.keystores.PemKeyStores;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Vertx;

@Recorder
public class CertificateRecorder implements TlsConfigurationRegistry {

    private final Map<String, TlsConfiguration> certificates = new ConcurrentHashMap<>();
    private volatile TlsCertificateUpdater reloader;
    private volatile Vertx vertx;

    private final RuntimeValue<TlsConfig> runtimeConfig;

    public CertificateRecorder(final RuntimeValue<TlsConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    /**
     * Validate the certificate configuration.
     * <p>
     * Verify that each certificate file exists and that the key store and trust store are correctly configured.
     * When aliases are set, aliases are validated.
     *
     * @param providerBucketNames the bucket names from {@link Identifier @Identifer} annotations on any
     *        {@link KeyStoreProvider} or {@link TrustStoreProvider} beans
     * @param vertx the Vert.x instance
     */
    public void validateCertificates(Set<String> providerBucketNames,
            RuntimeValue<Vertx> vertx,
            ShutdownContext shutdownContext) {
        this.vertx = vertx.getValue();
        // Verify the default config
        if (runtimeConfig.getValue().defaultCertificateConfig().isPresent()) {
            verifyCertificateConfig(runtimeConfig.getValue().defaultCertificateConfig().get(), vertx.getValue(),
                    TlsConfig.DEFAULT_NAME);
        }

        var bucketNames = new HashSet<>(runtimeConfig.getValue().namedCertificateConfig().keySet());
        bucketNames.addAll(providerBucketNames);

        // Verify the named configs
        for (String name : bucketNames) {
            if (name.equals(TlsConfig.DEFAULT_NAME)) {
                throw new IllegalArgumentException(
                        "The TLS configuration name " + TlsConfig.DEFAULT_NAME
                                + " cannot be used explicitly in configuration or qualifiers");
            }
            if (name.equals(TlsConfig.JAVA_NET_SSL_TLS_CONFIGURATION_NAME)) {
                throw new IllegalArgumentException(
                        "The TLS configuration name " + TlsConfig.JAVA_NET_SSL_TLS_CONFIGURATION_NAME
                                + " is reserved for providing access to default SunJSSE keystore; neither Quarkus extensions nor end users can adjust or override it");
            }
            verifyCertificateConfig(runtimeConfig.getValue().namedCertificateConfig().get(name), vertx.getValue(), name);
        }

        shutdownContext.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                if (reloader != null) {
                    reloader.close();
                }
            }
        });
    }

    private void verifyCertificateConfig(TlsBucketConfig config, Vertx vertx, String name) {
        final TlsConfiguration tlsConfig = verifyCertificateConfigInternal(config, vertx, name);
        certificates.put(name, tlsConfig);

        // Handle reloading if needed
        if (config.reloadPeriod().isPresent()) {
            if (reloader == null) {
                reloader = new TlsCertificateUpdater(vertx);
            }
            reloader.add(name, certificates.get(name), config.reloadPeriod().get());
        }
    }

    private static TlsConfiguration verifyCertificateConfigInternal(TlsBucketConfig config, Vertx vertx, String name) {
        // Verify the key store
        KeyStoreAndKeyCertOptions ks = getKeyStore(config, vertx, name);

        if (ks != null && config.keyStore().isPresent() && config.keyStore().get().sni()) {
            try {
                if (Collections.list(ks.keyStore.aliases()).size() <= 1) {
                    throw new IllegalStateException(
                            "The SNI option cannot be used when the keystore contains only one alias or the `alias` property has been set");
                }
            } catch (KeyStoreException e) {
                // Should not happen
                throw new RuntimeException(e);
            }
        }

        // Verify the trust store
        TrustStoreAndTrustOptions ts = getTrustStore(config, vertx, name);

        if (config.trustAll() && ts != null) {
            throw new IllegalStateException("The trust-all option cannot be used when a trust-store is configured");
        } else if (config.trustAll()) {
            ts = new TrustStoreAndTrustOptions(null, TrustAllOptions.INSTANCE);
        }
        return new VertxCertificateHolder(vertx, name, config, ks, ts);
    }

    public static KeyStoreAndKeyCertOptions getKeyStore(TlsBucketConfig bucketConfig, Vertx vertx, String name) {
        try (var providerInstance = lookupProvider(KeyStoreProvider.class, name)) {
            if (bucketConfig.keyStore().isPresent()) {
                var config = bucketConfig.keyStore().get();

                config.validate(providerInstance, name);

                if (config.pem().isPresent()) {
                    return PemKeyStores.verifyPEMKeyStore(config, vertx, name);
                } else if (config.p12().isPresent()) {
                    return P12KeyStores.verifyP12KeyStore(config, vertx, name);
                } else if (config.jks().isPresent()) {
                    return JKSKeyStores.verifyJKSKeyStore(config, vertx, name);
                }
            }

            if (providerInstance.isAvailable()) {
                return providerInstance.get().getKeyStore(vertx);
            }
        }

        return null;
    }

    public static TrustStoreAndTrustOptions getTrustStore(TlsBucketConfig bucketConfig, Vertx vertx, String name) {
        try (var providerInstance = lookupProvider(TrustStoreProvider.class, name)) {
            if (bucketConfig.trustStore().isPresent()) {
                var config = bucketConfig.trustStore().get();

                config.validate(providerInstance, name);

                if (config.pem().isPresent()) {
                    return PemKeyStores.verifyPEMTrustStoreStore(config, vertx, name);
                } else if (config.p12().isPresent()) {
                    return P12KeyStores.verifyP12TrustStoreStore(config, vertx, name);
                } else if (config.jks().isPresent()) {
                    return JKSKeyStores.verifyJKSTrustStoreStore(config, vertx, name);
                }
            }

            if (providerInstance.isAvailable()) {
                return providerInstance.get().getTrustStore(vertx);
            }
        }

        return null;
    }

    @Override
    public Optional<TlsConfiguration> get(String name) {
        if (TlsConfig.JAVA_NET_SSL_TLS_CONFIGURATION_NAME.equals(name)) {
            final TlsConfiguration result = certificates.computeIfAbsent(TlsConfig.JAVA_NET_SSL_TLS_CONFIGURATION_NAME, k -> {
                final TrustStoreAndTrustOptions ts = JavaxNetSslTrustStoreProvider.getTrustStore(vertx);
                return new VertxCertificateHolder(vertx, k, runtimeConfig.getValue().namedCertificateConfig().get(k), null, ts);
            });
            return Optional.ofNullable(result);
        }
        return Optional.ofNullable(certificates.get(name));
    }

    @Override
    public Optional<TlsConfiguration> getDefault() {
        return get(TlsConfig.DEFAULT_NAME);
    }

    @Override
    public void register(String name, TlsConfiguration configuration) {
        if (name == null) {
            throw new IllegalArgumentException("The name of the TLS configuration to register cannot be null");
        }
        if (name.equals(TlsConfig.DEFAULT_NAME)) {
            throw new IllegalArgumentException("The name of the TLS configuration to register cannot be <default>");
        }
        if (name.equals(TlsConfig.JAVA_NET_SSL_TLS_CONFIGURATION_NAME)) {
            throw new IllegalArgumentException(
                    "The TLS configuration name " + TlsConfig.JAVA_NET_SSL_TLS_CONFIGURATION_NAME
                            + " is reserved for providing access to default SunJSSE keystore; neither Quarkus extensions nor end users can adjust of override it");
        }
        if (configuration == null) {
            throw new IllegalArgumentException("The TLS configuration to register cannot be null");
        }
        certificates.put(name, configuration);
    }

    public Supplier<TlsConfigurationRegistry> getSupplier() {
        return new Supplier<TlsConfigurationRegistry>() {
            @Override
            public TlsConfigurationRegistry get() {
                return CertificateRecorder.this;
            }
        };
    }

    public void register(String name, Supplier<TlsConfiguration> supplier) {
        register(name, supplier.get());
    }

    static <T> InstanceHandle<T> lookupProvider(Class<T> type, String bucketName) {
        var container = Arc.container();
        var qualifier = TlsConfig.DEFAULT_NAME.equals(bucketName)
                ? Default.Literal.INSTANCE
                : Identifier.Literal.of(bucketName);
        var instances = container.listAll(type, qualifier);
        if (instances.size() > 1) {
            throw new AmbiguousResolutionException(
                    "multiple beans with type " + type.getName() + " found for TLS configuration " + bucketName);
        }
        if (instances.isEmpty()) {
            return new InstanceHandle<T>() {
                @Override
                public T get() {
                    return null;
                }
            };
        }
        return instances.get(0);
    }
}
