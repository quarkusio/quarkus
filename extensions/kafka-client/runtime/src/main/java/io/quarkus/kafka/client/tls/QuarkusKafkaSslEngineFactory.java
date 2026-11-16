package io.quarkus.kafka.client.tls;

import static io.quarkus.kafka.client.runtime.KafkaRuntimeConfigProducer.TLS_CONFIG_NAME_KEY;

import java.io.IOException;
import java.security.KeyStore;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SslEngineFactory;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;

public class QuarkusKafkaSslEngineFactory implements SslEngineFactory {

    private static final Logger log = Logger.getLogger(QuarkusKafkaSslEngineFactory.class);

    /**
     * Omits 'ssl.endpoint.identification.algorithm' because it is set by the user and it is not ignored
     */
    private static final Set<String> KAFKA_SSL_CONFIGS = Set.of(
            SslConfigs.SSL_KEYSTORE_TYPE_CONFIG,
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG,
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG,
            SslConfigs.SSL_KEY_PASSWORD_CONFIG,
            SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG,
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG,
            SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG,
            SslConfigs.SSL_KEYSTORE_KEY_CONFIG,
            SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG,
            SslConfigs.SSL_PROTOCOL_CONFIG,
            SslConfigs.SSL_PROVIDER_CONFIG,
            SslConfigs.SSL_CIPHER_SUITES_CONFIG,
            SslConfigs.SSL_ENABLED_PROTOCOLS_CONFIG,
            SslConfigs.SSL_KEYMANAGER_ALGORITHM_CONFIG,
            SslConfigs.SSL_TRUSTMANAGER_ALGORITHM_CONFIG,
            SslConfigs.SSL_SECURE_RANDOM_IMPLEMENTATION_CONFIG);

    private String tlsConfigName;
    private String clientId;
    private volatile TlsConfiguration configuration;
    /**
     * Rebuilt when the TLS registry reloads the configuration, see {@link #reload(TlsConfiguration)}.
     * Kafka calls {@link #createClientSslEngine} for each new connection, so connections opened after a reload
     * use the new key material while established connections are left untouched.
     */
    private volatile SSLContext sslContext;
    private volatile Runnable unregister;

    @Override
    public SSLEngine createClientSslEngine(String peerHost, int peerPort, String endpointIdentification) {
        SSLContext context = sslContext;
        if (context == null) {
            throw new IllegalStateException("The Kafka SSL engine factory is not configured or has been closed");
        }
        SSLEngine sslEngine = context.createSSLEngine(peerHost, peerPort);
        sslEngine.setUseClientMode(true);
        SSLParameters sslParameters = sslEngine.getSSLParameters();
        sslParameters.setEndpointIdentificationAlgorithm(endpointIdentification);
        sslEngine.setSSLParameters(sslParameters);
        return sslEngine;
    }

    @Override
    public SSLEngine createServerSslEngine(String peerHost, int peerPort) {
        throw new IllegalStateException("Server mode is not supported");
    }

    @Override
    public boolean shouldBeRebuilt(Map<String, Object> nextConfigs) {
        return false;
    }

    @Override
    public Set<String> reconfigurableConfigs() {
        return Set.of();
    }

    @Override
    public KeyStore keystore() {
        TlsConfiguration config = configuration;
        return config != null ? config.getKeyStore() : null;
    }

    @Override
    public KeyStore truststore() {
        TlsConfiguration config = configuration;
        return config != null ? config.getTrustStore() : null;
    }

    @Override
    public void close() throws IOException {
        Runnable unregister;
        // synchronized with reload() so a concurrent reload cannot resurrect the context after close
        synchronized (this) {
            unregister = this.unregister;
            this.unregister = null;
            this.sslContext = null;
            this.configuration = null;
        }
        if (unregister != null) {
            unregister.run();
        }
    }

    @Override
    public void configure(Map<String, ?> configs) {
        String tlsConfigName = (String) configs.get(TLS_CONFIG_NAME_KEY);
        if (tlsConfigName == null) {
            throw new IllegalArgumentException(
                    "The 'tls-configuration-name' property is required for Kafka Quarkus TLS Registry integration.");
        }

        TlsConfigurationRegistry registry = Arc.container().instance(TlsConfigurationRegistry.class).get();
        TlsConfiguration tlsConfiguration = registry.get(tlsConfigName)
                .orElseThrow(() -> new IllegalArgumentException("No TLS configuration found for name " + tlsConfigName));
        InstanceHandle<KafkaCertificateUpdateEventListener> listener = Arc.container()
                .instance(KafkaCertificateUpdateEventListener.class);
        String clientId = (String) configs.get(CommonClientConfigs.CLIENT_ID_CONFIG);
        configure(tlsConfigName, clientId, tlsConfiguration, listener.isAvailable() ? listener.get() : null);
    }

    void configure(String tlsConfigName, String clientId, TlsConfiguration tlsConfiguration,
            KafkaCertificateUpdateEventListener listener) {
        this.tlsConfigName = tlsConfigName;
        this.clientId = clientId;
        this.sslContext = createSslContext(tlsConfiguration);
        this.configuration = tlsConfiguration;
        if (listener != null) {
            this.unregister = listener.register(this);
        }
        log.debugf("Configured Kafka client '%s' QuarkusKafkaSslEngineFactory with TLS configuration : %s",
                clientId, tlsConfigName);
    }

    String tlsConfigName() {
        return tlsConfigName;
    }

    /**
     * Rebuilds the {@link SSLContext} from the reloaded TLS configuration.
     * <p>
     * Called when a {@link io.quarkus.tls.CertificateUpdatedEvent} is fired for the TLS configuration used by this
     * client. If the new context cannot be created, the previous one is kept so the client can still open connections.
     *
     * @param updatedConfiguration the reloaded TLS configuration
     * @return {@code true} if the context has been replaced
     */
    synchronized boolean reload(TlsConfiguration updatedConfiguration) {
        if (sslContext == null) {
            // closed
            return false;
        }
        try {
            SSLContext updated = createSslContext(updatedConfiguration);
            this.configuration = updatedConfiguration;
            this.sslContext = updated;
            log.infof("The TLS configuration '%s' used by the Kafka client '%s' has been reloaded; it applies to new"
                    + " connections", tlsConfigName, clientId);
            return true;
        } catch (RuntimeException e) {
            log.warnf(e, "Failed to reload the TLS configuration '%s' for the Kafka client '%s', keeping the previous one",
                    tlsConfigName, clientId);
            return false;
        }
    }

    private static SSLContext createSslContext(TlsConfiguration tlsConfiguration) {
        try {
            return tlsConfiguration.createSSLContext();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SSLContext", e);
        }
    }

    /**
     * Check if any SSL configuration is set for the Kafka client that will be ignored because the TLS configuration is set
     *
     * @param configs the Kafka client configuration
     */
    public static void checkForOtherSslConfigs(Map<String, ?> configs) {
        String tlsConfigName = (String) configs.get(TLS_CONFIG_NAME_KEY);
        for (String sslConfig : KAFKA_SSL_CONFIGS) {
            if (configs.containsKey(sslConfig)) {
                log.warnf(
                        "The SSL configuration '%s' is set for Kafka client '%s' but it will be ignored because the TLS configuration '%s' is set",
                        sslConfig, configs.get(CommonClientConfigs.CLIENT_ID_CONFIG), tlsConfigName);
            }
        }
    }
}
