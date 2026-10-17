package io.quarkus.kafka.client.tls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import org.junit.jupiter.api.Test;

import io.quarkus.tls.TlsConfiguration;

class QuarkusKafkaSslEngineFactoryTest {

    @Test
    void newConnectionsUseTlsMaterialAfterCertificateRotation() throws Exception {
        TlsConfiguration tlsConfiguration = mock(TlsConfiguration.class);
        SSLContext originalContext = mock(SSLContext.class);
        SSLContext rotatedContext = mock(SSLContext.class);
        SSLEngine originalEngine = mock(SSLEngine.class);
        SSLEngine rotatedEngine = mock(SSLEngine.class);
        SSLParameters originalParameters = new SSLParameters();
        SSLParameters rotatedParameters = new SSLParameters();

        when(tlsConfiguration.createSSLContext()).thenReturn(originalContext, rotatedContext);
        when(originalContext.createSSLEngine("broker.example", 9093)).thenReturn(originalEngine);
        when(rotatedContext.createSSLEngine("broker.example", 9093)).thenReturn(rotatedEngine);
        when(originalEngine.getSSLParameters()).thenReturn(originalParameters);
        when(rotatedEngine.getSSLParameters()).thenReturn(rotatedParameters);

        QuarkusKafkaSslEngineFactory factory = new QuarkusKafkaSslEngineFactory(tlsConfiguration);
        assertSame(originalEngine, factory.createClientSslEngine("broker.example", 9093, "HTTPS"));
        // After a registry reload, the same configuration returns a context with new key material.
        assertSame(rotatedEngine, factory.createClientSslEngine("broker.example", 9093, "HTTPS"));

        verify(tlsConfiguration, times(2)).createSSLContext();
        verify(originalEngine).setUseClientMode(true);
        verify(rotatedEngine).setUseClientMode(true);
        verify(originalEngine).setSSLParameters(originalParameters);
        verify(rotatedEngine).setSSLParameters(rotatedParameters);
        assertEquals("HTTPS", originalParameters.getEndpointIdentificationAlgorithm());
        assertEquals("HTTPS", rotatedParameters.getEndpointIdentificationAlgorithm());
    }

    @Test
    void reportsTlsContextCreationFailureWhenOpeningNewConnection() throws Exception {
        TlsConfiguration tlsConfiguration = mock(TlsConfiguration.class);
        IOException failure = new IOException("Updated certificate cannot be read");
        when(tlsConfiguration.createSSLContext()).thenThrow(failure);

        QuarkusKafkaSslEngineFactory factory = new QuarkusKafkaSslEngineFactory(tlsConfiguration);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> factory.createClientSslEngine("broker.example", 9093, "HTTPS"));

        assertSame(failure, error.getCause());
    }

    @Test
    void doesNotCreateAnEngineAfterTheFactoryIsClosed() throws Exception {
        TlsConfiguration tlsConfiguration = mock(TlsConfiguration.class);
        QuarkusKafkaSslEngineFactory factory = new QuarkusKafkaSslEngineFactory(tlsConfiguration);
        factory.close();

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> factory.createClientSslEngine("broker.example", 9093, "HTTPS"));

        assertEquals("Kafka SSL engine factory is not configured", error.getMessage());
    }
}
