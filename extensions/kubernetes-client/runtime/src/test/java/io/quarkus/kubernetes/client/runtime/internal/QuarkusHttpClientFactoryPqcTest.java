package io.quarkus.kubernetes.client.runtime.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.config.Config;
import io.vertx.core.http.PoolOptions;
import io.vertx.core.http.WebSocketClientOptions;
import io.vertx.core.net.PqcEnforcementPolicy;
import io.vertx.ext.web.client.WebClientOptions;

class QuarkusHttpClientFactoryPqcTest {

    private Config config;
    private QuarkusHttpClientFactory factory;

    @BeforeEach
    void setUp() {
        config = Config.getOrCreate();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("quarkus.kubernetes-client.pqc-enforcement-policy");
        System.clearProperty("quarkus.kubernetes-client.key-exchange-groups");
        ConfigProviderResolver.instance().releaseConfig(config);
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    void strictPolicyShouldBeAppliedToBothHttpAndWebSocketOptions() {
        System.setProperty("quarkus.kubernetes-client.pqc-enforcement-policy", "strict");
        System.setProperty("quarkus.kubernetes-client.key-exchange-groups", "X25519MLKEM768");
        factory = new QuarkusHttpClientFactory();

        WebClientOptions httpOptions = new WebClientOptions();
        httpOptions.setEnabledSecureTransportProtocols(Set.of("TLSv1.3"));
        WebSocketClientOptions wsOptions = new WebSocketClientOptions();
        wsOptions.setEnabledSecureTransportProtocols(Set.of("TLSv1.3"));

        factory.additionalConfig(httpOptions, wsOptions, new PoolOptions());

        assertEquals(PqcEnforcementPolicy.STRICT, httpOptions.getSslOptions().getPqcEnforcementPolicy());
        assertEquals(List.of("X25519MLKEM768"), httpOptions.getSslOptions().getKeyExchangeGroups());

        assertEquals(PqcEnforcementPolicy.STRICT, wsOptions.getSslOptions().getPqcEnforcementPolicy());
        assertEquals(List.of("X25519MLKEM768"), wsOptions.getSslOptions().getKeyExchangeGroups());
    }

    @Test
    void clientNegotiatedPolicyShouldBeAppliedToBothHttpAndWebSocketOptions() {
        System.setProperty("quarkus.kubernetes-client.pqc-enforcement-policy", "client-negotiated");
        System.setProperty("quarkus.kubernetes-client.key-exchange-groups", "X25519MLKEM768,X25519");
        factory = new QuarkusHttpClientFactory();

        WebClientOptions httpOptions = new WebClientOptions();
        httpOptions.setEnabledSecureTransportProtocols(Set.of("TLSv1.3"));
        WebSocketClientOptions wsOptions = new WebSocketClientOptions();
        wsOptions.setEnabledSecureTransportProtocols(Set.of("TLSv1.3"));

        factory.additionalConfig(httpOptions, wsOptions, new PoolOptions());

        assertEquals(PqcEnforcementPolicy.CLIENT_NEGOTIATED, httpOptions.getSslOptions().getPqcEnforcementPolicy());
        assertEquals(List.of("X25519MLKEM768", "X25519"), httpOptions.getSslOptions().getKeyExchangeGroups());

        assertEquals(PqcEnforcementPolicy.CLIENT_NEGOTIATED, wsOptions.getSslOptions().getPqcEnforcementPolicy());
        assertEquals(List.of("X25519MLKEM768", "X25519"), wsOptions.getSslOptions().getKeyExchangeGroups());
    }

    @Test
    void relaxedPolicyWithGroupsShouldBeAppliedToBothHttpAndWebSocketOptions() {
        System.setProperty("quarkus.kubernetes-client.pqc-enforcement-policy", "relaxed");
        System.setProperty("quarkus.kubernetes-client.key-exchange-groups", "X25519MLKEM768");
        factory = new QuarkusHttpClientFactory();

        WebClientOptions httpOptions = new WebClientOptions();
        httpOptions.setEnabledSecureTransportProtocols(Set.of("TLSv1.3"));
        WebSocketClientOptions wsOptions = new WebSocketClientOptions();
        wsOptions.setEnabledSecureTransportProtocols(Set.of("TLSv1.3"));

        factory.additionalConfig(httpOptions, wsOptions, new PoolOptions());

        assertEquals(List.of("X25519MLKEM768"), httpOptions.getSslOptions().getKeyExchangeGroups());
        assertEquals(List.of("X25519MLKEM768"), wsOptions.getSslOptions().getKeyExchangeGroups());
    }

    @Test
    void noSslOptionsShouldBeSetWhenHttpOptionsHasNoSsl() {
        // When the Kubernetes API server URL is plain HTTP, getSslOptions() is null;
        // additionalConfig must not throw.
        System.setProperty("quarkus.kubernetes-client.pqc-enforcement-policy", "strict");
        System.setProperty("quarkus.kubernetes-client.key-exchange-groups", "X25519MLKEM768");
        factory = new QuarkusHttpClientFactory();

        // No SSL options initialised — getSslOptions() returns null
        WebClientOptions httpOptions = new WebClientOptions();

        factory.additionalConfig(httpOptions, new WebSocketClientOptions(), new PoolOptions());

        assertNull(httpOptions.getSslOptions());
    }
}
