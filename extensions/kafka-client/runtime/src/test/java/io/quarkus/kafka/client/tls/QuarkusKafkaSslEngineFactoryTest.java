package io.quarkus.kafka.client.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.tls.CertificateUpdatedEvent;
import io.quarkus.tls.TlsConfiguration;

class QuarkusKafkaSslEngineFactoryTest {

    private static final String HOST = "broker.example";
    private static final int PORT = 9093;

    private KafkaCertificateUpdateEventListener listener;
    private TlsConfiguration tlsConfiguration;
    private SSLContext initialContext;
    private SSLContext reloadedContext;
    private SSLEngine initialEngine;
    private SSLEngine reloadedEngine;

    @BeforeEach
    void setUp() throws Exception {
        listener = new KafkaCertificateUpdateEventListener();
        tlsConfiguration = mock(TlsConfiguration.class);
        initialContext = mock(SSLContext.class);
        reloadedContext = mock(SSLContext.class);
        initialEngine = engine(initialContext);
        reloadedEngine = engine(reloadedContext);
    }

    @Test
    void reusesTheSameContextForAllConnectionsUntilReload() throws Exception {
        when(tlsConfiguration.createSSLContext()).thenReturn(initialContext);
        QuarkusKafkaSslEngineFactory factory = configuredFactory("kafka-tls");

        assertThat(factory.createClientSslEngine(HOST, PORT, "HTTPS")).isSameAs(initialEngine);
        assertThat(factory.createClientSslEngine(HOST, PORT, "HTTPS")).isSameAs(initialEngine);

        verify(tlsConfiguration, times(1)).createSSLContext();
        verify(initialEngine, times(2)).setUseClientMode(true);
    }

    @Test
    void newConnectionsUseTheReloadedMaterial() throws Exception {
        when(tlsConfiguration.createSSLContext()).thenReturn(initialContext, reloadedContext);
        QuarkusKafkaSslEngineFactory factory = configuredFactory("kafka-tls");
        assertThat(factory.createClientSslEngine(HOST, PORT, "HTTPS")).isSameAs(initialEngine);

        listener.onCertificateUpdate(new CertificateUpdatedEvent("kafka-tls", tlsConfiguration));

        SSLEngine engine = factory.createClientSslEngine(HOST, PORT, "HTTPS");
        assertThat(engine).isSameAs(reloadedEngine);
        assertThat(engine.getSSLParameters().getEndpointIdentificationAlgorithm()).isEqualTo("HTTPS");
        verify(tlsConfiguration, times(2)).createSSLContext();
    }

    @Test
    void keystoreAndTruststoreFollowTheReloadedConfiguration() throws Exception {
        TlsConfiguration reloadedConfiguration = mock(TlsConfiguration.class);
        KeyStore reloadedKeyStore = mock(KeyStore.class);
        KeyStore reloadedTrustStore = mock(KeyStore.class);
        when(tlsConfiguration.createSSLContext()).thenReturn(initialContext);
        when(reloadedConfiguration.createSSLContext()).thenReturn(reloadedContext);
        when(reloadedConfiguration.getKeyStore()).thenReturn(reloadedKeyStore);
        when(reloadedConfiguration.getTrustStore()).thenReturn(reloadedTrustStore);
        QuarkusKafkaSslEngineFactory factory = configuredFactory("kafka-tls");

        listener.onCertificateUpdate(new CertificateUpdatedEvent("kafka-tls", reloadedConfiguration));

        assertThat(factory.keystore()).isSameAs(reloadedKeyStore);
        assertThat(factory.truststore()).isSameAs(reloadedTrustStore);
    }

    @Test
    void ignoresReloadOfAnotherTlsConfiguration() throws Exception {
        when(tlsConfiguration.createSSLContext()).thenReturn(initialContext, reloadedContext);
        QuarkusKafkaSslEngineFactory factory = configuredFactory("kafka-tls");

        listener.onCertificateUpdate(new CertificateUpdatedEvent("http-tls", mock(TlsConfiguration.class)));

        assertThat(factory.createClientSslEngine(HOST, PORT, "HTTPS")).isSameAs(initialEngine);
        verify(tlsConfiguration, times(1)).createSSLContext();
    }

    @Test
    void keepsThePreviousContextWhenTheReloadFails() throws Exception {
        TlsConfiguration brokenConfiguration = mock(TlsConfiguration.class);
        when(tlsConfiguration.createSSLContext()).thenReturn(initialContext);
        when(brokenConfiguration.createSSLContext()).thenThrow(new IllegalStateException("cannot read certificate"));
        QuarkusKafkaSslEngineFactory factory = configuredFactory("kafka-tls");

        assertThat(factory.reload(brokenConfiguration)).isFalse();

        assertThat(factory.createClientSslEngine(HOST, PORT, "HTTPS")).isSameAs(initialEngine);
    }

    @Test
    void closeUnregistersTheFactory() throws Exception {
        when(tlsConfiguration.createSSLContext()).thenReturn(initialContext, reloadedContext);
        QuarkusKafkaSslEngineFactory factory = configuredFactory("kafka-tls");
        QuarkusKafkaSslEngineFactory other = configuredFactory("kafka-tls");
        assertThat(listener.registrations()).isEqualTo(2);

        factory.close();

        assertThat(listener.registrations()).isEqualTo(1);
        assertThat(factory.reload(tlsConfiguration)).isFalse();
        assertThatThrownBy(() -> factory.createClientSslEngine(HOST, PORT, "HTTPS"))
                .isInstanceOf(IllegalStateException.class);
        other.close();
        assertThat(listener.registrations()).isZero();
    }

    @Test
    void failsFastWhenTheInitialContextCannotBeCreated() throws Exception {
        when(tlsConfiguration.createSSLContext()).thenThrow(new IllegalStateException("cannot read certificate"));
        QuarkusKafkaSslEngineFactory factory = new QuarkusKafkaSslEngineFactory();

        assertThatThrownBy(() -> factory.configure("kafka-tls", "client", tlsConfiguration, listener))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create SSLContext");
        assertThat(listener.registrations()).isZero();
    }

    private QuarkusKafkaSslEngineFactory configuredFactory(String tlsConfigName) {
        QuarkusKafkaSslEngineFactory factory = new QuarkusKafkaSslEngineFactory();
        factory.configure(tlsConfigName, "client", tlsConfiguration, listener);
        return factory;
    }

    private static SSLEngine engine(SSLContext context) {
        SSLEngine engine = mock(SSLEngine.class);
        SSLParameters parameters = new SSLParameters();
        when(context.createSSLEngine(HOST, PORT)).thenReturn(engine);
        when(engine.getSSLParameters()).thenReturn(parameters);
        return engine;
    }
}
