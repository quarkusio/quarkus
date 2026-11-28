package io.quarkus.spiffe.client.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;

@QuarkusTestResource(SpiffeX509TestResource.class)
public class SpiffeX509SvidRefreshTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClass(TlsConfigurations.class))
            .setForcedDependencies(
                    List.of(Dependency.of("io.quarkus", "quarkus-tls-registry-deployment", Version.getVersion())))
            .overrideRuntimeConfigKey("quarkus.spiffe-client.endpoint-socket",
                    "${x509.quarkus.spiffe-client.endpoint-socket}")
            .overrideRuntimeConfigKey("quarkus.spiffe-client.x509-svid.refresh-period", "1s")
            // the unchanged SVID is only visible in the trace log
            .overrideConfigKey("quarkus.log.category.\"io.quarkus.spiffe.client.runtime.internal.tls\".min-level", "TRACE")
            .overrideRuntimeConfigKey("quarkus.log.category.\"io.quarkus.spiffe.client.runtime.internal.tls\".level",
                    "TRACE")
            .setLogRecordPredicate(record -> record.getLoggerName().startsWith("io.quarkus.spiffe.client.runtime"))
            .assertLogRecords(records -> {
                List<String> messages = records.stream().map(LogRecord::getMessage).toList();
                assertThat(messages).anyMatch(m -> m.contains("exceeds a quarter of the SVID lifetime"));
                assertThat(messages).anyMatch(m -> m.contains("expired at"));
                assertThat(messages).anyMatch(m -> m.contains("no longer issues an X.509-SVID"));
                assertThat(messages).anyMatch(m -> m.contains("works again"));
                assertThat(messages).anyMatch(m -> m.contains("is unchanged"));
            });

    @X509Svid(tls = "spiffe-refresh")
    static class TlsConfigurations {
    }

    @Inject
    TlsConfigurationRegistry registry;

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "spiffe.x509.test.admin-url")
    String adminUrl;

    private TlsConfiguration configuration;
    private HttpServer server;

    @BeforeEach
    void startServer() throws Exception {
        configuration = registry.get("spiffe-refresh").orElseThrow();
        server = vertx.createHttpServer(new HttpServerOptions()
                .setSsl(true)
                .setKeyCertOptions(configuration.getKeyStoreOptions())
                .setTrustOptions(configuration.getTrustStoreOptions())
                .setClientAuth(ClientAuth.REQUIRED))
                .requestHandler(request -> request.response().end());
        server.listen(0, "localhost").await(TIMEOUT);
    }

    @AfterEach
    void healthyAgentAgain() throws Exception {
        admin(SpiffeX509TestResource.X509_SVID_TTL_PATH, "P1D");
        admin(SpiffeX509TestResource.X509_SVID_REUSE_PATH, "false");
        admin(SpiffeX509TestResource.MODE_PATH, "HEALTHY");
        await().atMost(TIMEOUT).ignoreExceptions()
                .untilAsserted(() -> assertThat(lifetime(presentedCertificate())).isGreaterThan(TIMEOUT));
        server.close().await(TIMEOUT);
    }

    @Test
    void rotatedSvidIsPickedUp() throws Exception {
        // the fake Workload API issues a new leaf on every fetch, so a refresh period later the server presents another one
        X509Certificate first = presentedCertificate();
        await().atMost(TIMEOUT).ignoreExceptions()
                .untilAsserted(() -> assertThat(presentedCertificate()).isNotEqualTo(first));
    }

    @Test
    void unchangedSvidIsKept() throws Exception {
        admin(SpiffeX509TestResource.X509_SVID_REUSE_PATH, "true");
        // once the reused SVID is current, the refreshes keep presenting it
        await().pollDelay(Duration.ofMillis(1500)).atMost(TIMEOUT).until(() -> true);
        X509Certificate reused = presentedCertificate();
        await().during(Duration.ofMillis(2500)).atMost(TIMEOUT).ignoreExceptions()
                .until(() -> presentedCertificate().equals(reused));
    }

    @Test
    void expiredSvidIsDroppedUntilAReplacementArrives() throws Exception {
        admin(SpiffeX509TestResource.X509_SVID_TTL_PATH, "PT3S");
        // the next refresh brings a three-second SVID, and the refresh period of one second exceeds a quarter of it
        await().atMost(TIMEOUT).ignoreExceptions().untilAsserted(
                () -> assertThat(lifetime(presentedCertificate())).isLessThanOrEqualTo(Duration.ofSeconds(3)));
        admin(SpiffeX509TestResource.MODE_PATH, "GRPC_DOWN");
        // no replacement can be fetched: once the SVID expired, the handshake has no certificate and no trust bundle
        await().atMost(TIMEOUT).until(() -> !handshakeWorks());
        admin(SpiffeX509TestResource.X509_SVID_TTL_PATH, "P1D");
        admin(SpiffeX509TestResource.MODE_PATH, "HEALTHY");
        await().atMost(TIMEOUT).ignoreExceptions()
                .untilAsserted(() -> assertThat(lifetime(presentedCertificate())).isGreaterThan(TIMEOUT));
    }

    @Test
    void deniedWorkloadStopsUsingItsSvid() throws Exception {
        presentedCertificate();
        admin(SpiffeX509TestResource.MODE_PATH, "PERMISSION_DENIED");
        await().atMost(TIMEOUT).until(() -> !handshakeWorks());
        admin(SpiffeX509TestResource.MODE_PATH, "HEALTHY");
        await().atMost(TIMEOUT).until(this::handshakeWorks);
    }

    @Test
    void deniedWorkloadCannotResumeASession() throws Exception {
        // without keep-alive every request opens a connection, the second one resumes the session of the first
        HttpClient client = vertx.createHttpClient(new HttpClientOptions()
                .setSsl(true)
                .setKeepAlive(false)
                .setKeyCertOptions(configuration.getKeyStoreOptions())
                .setTrustOptions(configuration.getTrustStoreOptions()));
        try {
            assertThat(get(client)).isEqualTo(200);
            admin(SpiffeX509TestResource.MODE_PATH, "PERMISSION_DENIED");
            await().atMost(TIMEOUT).until(() -> !handshakeWorks());
            assertThatThrownBy(() -> get(client)).hasStackTraceContaining("no X.509-SVID is available");
        } finally {
            client.close().await(TIMEOUT);
        }
    }

    private int get(HttpClient client) throws Exception {
        return client.request(HttpMethod.GET, server.actualPort(), "localhost", "/")
                .compose(HttpClientRequest::send)
                .await(TIMEOUT)
                .statusCode();
    }

    // a fresh client per handshake, so no TLS session is resumed and the server has to present its current certificate
    private X509Certificate presentedCertificate() throws Exception {
        NetClient client = vertx.createNetClient(new NetClientOptions()
                .setSsl(true)
                .setHostnameVerificationAlgorithm("HTTPS") // the SPIFFE trust manager replaces it, the SVID has no DNS name
                .setKeyCertOptions(configuration.getKeyStoreOptions())
                .setTrustOptions(configuration.getTrustStoreOptions()));
        try {
            NetSocket socket = client.connect(server.actualPort(), "localhost")
                    .await(TIMEOUT);
            try {
                return (X509Certificate) socket.peerCertificates().getFirst();
            } finally {
                socket.close();
            }
        } finally {
            client.close().await(TIMEOUT);
        }
    }

    private boolean handshakeWorks() {
        try {
            presentedCertificate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static Duration lifetime(X509Certificate certificate) {
        return Duration.between(certificate.getNotBefore().toInstant(), certificate.getNotAfter().toInstant());
    }

    private void admin(String path, String body) throws Exception {
        URI uri = URI.create(adminUrl);
        HttpClient client = vertx.createHttpClient();
        try {
            int status = client.request(HttpMethod.POST, uri.getPort(), uri.getHost(), path)
                    .compose(request -> request.send(Buffer.buffer(body)))
                    .await(TIMEOUT)
                    .statusCode();
            assertThat(status).isEqualTo(200);
        } finally {
            client.close().await(TIMEOUT);
        }
    }
}
