package io.quarkus.spiffe.client.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.spiffe.svid.x509.X509Svid;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;

@QuarkusTestResource(SpiffeX509TestResource.class)
class SpiffeX509SvidTlsRegistryTest {

    private static final String WORKLOAD_SPIFFE_ID = "spiffe://test.quarkus.io/test-workload";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClass(TlsConfigurations.class))
            .setForcedDependencies(
                    List.of(Dependency.of("io.quarkus", "quarkus-tls-registry-deployment", Version.getVersion())))
            .overrideRuntimeConfigKey("quarkus.spiffe-client.endpoint-socket", "${x509.quarkus.spiffe-client.endpoint-socket}")
            .overrideRuntimeConfigKey("spiffe.test.peer", WORKLOAD_SPIFFE_ID)
            .overrideRuntimeConfigKey("quarkus.tls.spiffe-peer.handshake-timeout", "5s");

    @X509Svid(tls = "spiffe-any")
    @X509Svid(tls = "spiffe-peer", spiffeId = "${spiffe.test.peer}")
    @X509Svid(tls = "spiffe-other", spiffeId = "spiffe://test.quarkus.io/other-workload")
    static class TlsConfigurations {
    }

    @Inject
    TlsConfigurationRegistry registry;

    @Inject
    Vertx vertx;

    @Test
    void registryBuildsTheConfigurationsFromTheProviders() {
        TlsConfiguration any = registry.get("spiffe-any").orElseThrow();
        assertThat(any.getKeyStoreOptions().getClass().getSimpleName()).isEqualTo("SpiffeKeyManager");
        assertThat(any.getTrustStoreOptions().getClass().getSimpleName()).isEqualTo("SpiffeTrustManager");
        assertThat(any.getKeyStore()).isNull();
        assertThat(any.getTrustStore()).isNull();
        assertThat(any.isTrustAll()).isFalse();
        // the registry defaults apply when nothing is configured for the name
        assertThat(any.getClientSSLOptions().getSslHandshakeTimeout()).isEqualTo(10L);
        // and 'quarkus.tls.<name>.*' applies when it is
        TlsConfiguration peer = registry.get("spiffe-peer").orElseThrow();
        assertThat(peer.getClientSSLOptions().getSslHandshakeTimeout()).isEqualTo(5L);
        assertThat(peer.getClientSSLOptions().getSslHandshakeTimeoutUnit()).isEqualTo(TimeUnit.SECONDS);
        assertThat(registry.get("spiffe-other")).isPresent();
    }

    @Test
    void mutualTlsThroughTheRegistry() throws Exception {
        TlsConfiguration serverConfiguration = registry.get("spiffe-any").orElseThrow();
        HttpServer server = vertx.createHttpServer(new HttpServerOptions()
                .setSsl(true)
                .setKeyCertOptions(serverConfiguration.getKeyStoreOptions())
                .setTrustOptions(serverConfiguration.getTrustStoreOptions())
                .setClientAuth(ClientAuth.REQUIRED))
                .requestHandler(request -> {
                    try {
                        X509Certificate peer = (X509Certificate) request.connection().peerCertificates().get(0);
                        request.response().end(peer.getSubjectAlternativeNames().iterator().next().get(1).toString());
                    } catch (Exception e) {
                        request.response().setStatusCode(500).end(e.toString());
                    }
                });
        server.listen(0, "localhost").await(TIMEOUT);
        try {
            // the client authorizes the server SPIFFE ID, the server authorizes any workload of the trust domain;
            // the SVID carries no DNS name, so this also shows that hostname verification does not get in the way
            TlsConfiguration clientConfiguration = registry.get("spiffe-peer").orElseThrow();
            assertThat(get(clientConfiguration, server.actualPort())).isEqualTo(WORKLOAD_SPIFFE_ID);

            // TLS 1.3 only, even where the peer would allow TLS 1.2
            assertThatThrownBy(() -> get(clientConfiguration, server.actualPort(),
                    new HttpClientOptions().setEnabledSecureTransportProtocols(Set.of("TLSv1.2"))))
                    .hasStackTraceContaining("negotiated TLSv1.2, TLS 1.3 is required");

            TlsConfiguration unauthorized = registry.get("spiffe-other").orElseThrow();
            assertThatThrownBy(() -> get(unauthorized, server.actualPort()))
                    .hasStackTraceContaining("is not authorized by the TLS configuration 'spiffe-other'");
        } finally {
            server.close().await(TIMEOUT);
        }
    }

    private String get(TlsConfiguration configuration, int port) throws Exception {
        return get(configuration, port, new HttpClientOptions());
    }

    private String get(TlsConfiguration configuration, int port, HttpClientOptions options) throws Exception {
        HttpClient client = vertx.createHttpClient(options
                .setSsl(true)
                .setKeyCertOptions(configuration.getKeyStoreOptions())
                .setTrustOptions(configuration.getTrustStoreOptions()));
        try {
            return client.request(HttpMethod.GET, port, "localhost", "/")
                    .compose(request -> request.send())
                    .compose(response -> response.body())
                    .await(TIMEOUT)
                    .toString();
        } finally {
            client.close().await(TIMEOUT);
        }
    }
}
