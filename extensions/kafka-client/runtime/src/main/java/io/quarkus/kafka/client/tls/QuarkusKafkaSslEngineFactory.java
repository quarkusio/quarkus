package io.quarkus.kafka.client.tls;

import static io.quarkus.kafka.client.runtime.KafkaRuntimeConfigProducer.TLS_CONFIG_NAME_KEY;

import java.io.IOException;
import java.security.KeyStore;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SslEngineFactory;
import org.jboss.logging.Logger;

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

    private TlsConfiguration configuration;
    private SSLContext sslContext;

    @Override
    public SSLEngine createClientSslEngine(String peerHost, int peerPort, String endpointIdentification) {
        SSLEngine sslEngine = sslContext.createSSLEngine(peerHost, peerPort);
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
        return configuration.getKeyStore();
    }

    @Override
    public KeyStore truststore() {
        return configuration.getTrustStore();
    }

    @Override
    public void close() throws IOException {
        this.sslContext = null;
        this.configuration = null;
    }

    @Override
    public void configure(Map<String, ?> configs) {
        String tlsConfigName = (String) configs.get(TLS_CONFIG_NAME_KEY);
        if (tlsConfigName == null) {
            throw new IllegalArgumentException(
                    "The 'tls-configuration-name' property is required for Kafka Quarkus TLS Registry integration.");
        }

        Instance<TlsConfigurationRegistry> tlsConfig = CDI.current().getBeanManager().createInstance()
                .select(TlsConfigurationRegistry.class);
        if (!tlsConfig.isUnsatisfied()) {
            TlsConfigurationRegistry registry = tlsConfig.get();
            configuration = registry.get(tlsConfigName)
                    .orElseThrow(() -> new IllegalArgumentException("No TLS configuration found for name " + tlsConfigName));
            try {
                sslContext = configuration.createSSLContext();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create SSLContext", e);
            }
            String clientId = (String) configs.get(CommonClientConfigs.CLIENT_ID_CONFIG);
            log.debugf("Configured Kafka client '%s' QuarkusKafkaSslEngineFactory with TLS configuration : %s",
                    clientId, tlsConfigName);
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
