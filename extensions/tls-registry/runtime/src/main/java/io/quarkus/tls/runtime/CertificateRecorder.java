package io.quarkus.tls.runtime;

import java.security.KeyStoreException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.tls.runtime.config.KeyStoreConfig;
import io.quarkus.tls.runtime.config.TlsBucketConfig;
import io.quarkus.tls.runtime.config.TlsConfig;
import io.quarkus.tls.runtime.config.TrustStoreConfig;
import io.quarkus.tls.runtime.keystores.JKSKeyStores;
import io.quarkus.tls.runtime.keystores.P12KeyStores;
import io.quarkus.tls.runtime.keystores.PemKeyStores;
import io.quarkus.tls.runtime.keystores.TrustAllOptions;
import io.vertx.core.Vertx;

@Recorder
public class CertificateRecorder implements TlsConfigurationRegistry {

    private final Map<String, TlsConfiguration> certificates = new ConcurrentHashMap<>();
    private volatile TlsCertificateUpdater reloader;

    /**
     * Validate the certificate configuration.
     * <p>
     * Verify that each certificate file exists and that the key store and trust store are correctly configured.
     * When aliases are set, aliases are validated.
     *
     * @param config the configuration
     * @param vertx the Vert.x instance
     */
    public void validateCertificates(TlsConfig config, RuntimeValue<Vertx> vertx, ShutdownContext shutdownContext) {
        // Verify the default config
        if (config.defaultCertificateConfig().isPresent()) {
            verifyCertificateConfig(config.defaultCertificateConfig().get(), vertx.getValue(), TlsConfig.DEFAULT_NAME);
        }

        // Verify the named config
        for (String name : config.namedCertificateConfig().keySet()) {
            verifyCertificateConfig(config.namedCertificateConfig().get(name), vertx.getValue(), name);
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

    public void verifyCertificateConfig(TlsBucketConfig config, Vertx vertx, String name) {
        // Verify the key store
        KeyStoreAndKeyCertOptions ks = null;
        boolean sni;
        if (config.keyStore().isPresent()) {
            KeyStoreConfig keyStoreConfig = config.keyStore().get();
            ks = verifyKeyStore(keyStoreConfig, vertx, name);
            sni = keyStoreConfig.sni();
            if (sni && ks != null) {
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
        }

        // Verify the trust store
        TrustStoreAndTrustOptions ts = null;
        if (config.trustStore().isPresent()) {
            ts = verifyTrustStore(config.trustStore().get(), vertx, name);
        }

        if (config.trustAll() && ts != null) {
            throw new IllegalStateException("The trust-all option cannot be used when a trust-store is configured");
        } else if (config.trustAll()) {
            ts = new TrustStoreAndTrustOptions(null, TrustAllOptions.INSTANCE);
        }

        certificates.put(name, new VertxCertificateHolder(vertx, name, config, ks, ts));

        // Handle reloading if needed
        if (config.reloadPeriod().isPresent()) {
            if (reloader == null) {
                reloader = new TlsCertificateUpdater(vertx);
            }
            reloader.add(name, certificates.get(name), config.reloadPeriod().get());
        }
    }

    public static KeyStoreAndKeyCertOptions verifyKeyStore(KeyStoreConfig config, Vertx vertx, String name) {
        config.validate(name);

        if (config.pem().isPresent()) {
            return PemKeyStores.verifyPEMKeyStore(config, vertx, name);
        } else if (config.p12().isPresent()) {
            return P12KeyStores.verifyP12KeyStore(config, vertx, name);
        } else if (config.jks().isPresent()) {
            return JKSKeyStores.verifyJKSKeyStore(config, vertx, name);
        }
        return null;
    }

    public static TrustStoreAndTrustOptions verifyTrustStore(TrustStoreConfig config, Vertx vertx, String name) {
        config.validate(name);

        if (config.pem().isPresent()) {
            return PemKeyStores.verifyPEMTrustStoreStore(config, vertx, name);
        } else if (config.p12().isPresent()) {
            return P12KeyStores.verifyP12TrustStoreStore(config, vertx, name);
        } else if (config.jks().isPresent()) {
            return JKSKeyStores.verifyJKSTrustStoreStore(config, vertx, name);
        }

        return null;
    }

    @Override
    public Optional<TlsConfiguration> get(String name) {
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
}
