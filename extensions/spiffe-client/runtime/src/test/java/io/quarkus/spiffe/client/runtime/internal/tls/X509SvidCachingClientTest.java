package io.quarkus.spiffe.client.runtime.internal.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.spiffe.client.SpiffeAuthorizationException;
import io.quarkus.spiffe.client.SpiffeConnectionException;
import io.quarkus.spiffe.client.runtime.internal.tls.X509SvidCachingClient.X509SvidCachingClientImpl;
import io.quarkus.spiffe.client.runtime.internal.tls.X509SvidCachingClient.X509SvidCachingClientImpl.Timing;
import io.quarkus.spiffe.svid.x509.WorkloadCertificateChain;
import io.quarkus.spiffe.svid.x509.WorkloadCertificateDocument;
import io.quarkus.spiffe.svid.x509.WorkloadTrustBundle;
import io.quarkus.spiffe.svid.x509.X509SvidClient;
import io.smallrye.certs.CertificateUtils;
import io.smallrye.certs.KeyAlgorithm;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.vertx.core.Vertx;
import io.vertx.grpc.client.InvalidStatusException;
import io.vertx.grpc.common.GrpcStatus;

class X509SvidCachingClientTest {

    private static final String SPIFFE_ID = "spiffe://test.quarkus.io/workload";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    // startup gives up after two seconds, a failed tick is retried after half a second when the period is a second or more
    private static final Timing FAST = new Timing(Duration.ofSeconds(2), Duration.ofMillis(100), Duration.ofMillis(200),
            Duration.ofMillis(500), Duration.ofSeconds(1));

    private static Vertx vertx;
    private static PrivateKey key;
    private static X509Certificate leaf;
    private static X509Certificate rotatedLeaf;
    private static X509Certificate expiredLeaf;

    private X509SvidCachingClientImpl client;
    private final List<Runnable> stops = new ArrayList<>();

    @BeforeAll
    static void certificates() throws Exception {
        vertx = Vertx.vertx();
        KeyPair keyPair = KeyAlgorithm.EC_P256.generateKeyPair();
        key = keyPair.getPrivate();
        leaf = certificate(keyPair, Duration.ofDays(1));
        rotatedLeaf = certificate(keyPair, Duration.ofDays(2));
        expiredLeaf = certificate(keyPair, Duration.ZERO); // notAfter is now
    }

    @AfterAll
    static void closeVertx() throws Exception {
        vertx.close().await(TIMEOUT);
    }

    @AfterEach
    void stopAndClose() {
        stops.forEach(Runnable::run);
        if (client != null) {
            client.close();
        }
    }

    @Test
    void ticksAreSkippedWhileAFetchIsInFlight() {
        StubClient agent = new StubClient(document(leaf));
        client = start(agent, FAST, Duration.ofMillis(100));
        agent.hold = true;
        await().atMost(TIMEOUT).until(() -> agent.pending.size() == 1);
        await().pollDelay(Duration.ofMillis(600)).until(() -> true); // several ticks later
        assertThat(agent.fetches).hasValue(2); // the startup fetch and the pending tick only
        agent.hold = false;
        agent.release();
        await().atMost(TIMEOUT).until(() -> agent.fetches.get() >= 3); // the ticks fetch again
    }

    @Test
    void startupRetriesWithBackoff() {
        StubClient agent = new StubClient(document(leaf));
        agent.failuresLeft = 2;
        client = start(agent, FAST, Duration.ofMinutes(5));
        assertThat(agent.fetches).hasValue(3);
        assertThat(client.getCurrentWorkloadCertificate()).isSameAs(agent.document);
    }

    @Test
    void startupGivesUpAfterTheTimeout() {
        StubClient agent = new StubClient(document(leaf));
        agent.failuresLeft = Integer.MAX_VALUE;
        client = new X509SvidCachingClientImpl(agent, FAST);
        assertThatThrownBy(() -> initialize(client, Duration.ofMinutes(5)).await().atMost(TIMEOUT))
                .isInstanceOf(CompletionException.class)
                .cause().isInstanceOf(SpiffeConnectionException.class)
                .hasMessageContaining("within PT2S")
                .hasCauseInstanceOf(SpiffeConnectionException.class);
        assertThat(agent.fetches.get()).isGreaterThan(3);
        assertThat(client.getCurrentWorkloadCertificate()).isNull();
    }

    @Test
    void unimplementedMethodIsNotRetried() {
        StubClient agent = new StubClient(document(leaf));
        agent.failure = new SpiffeConnectionException("UNIMPLEMENTED",
                new InvalidStatusException(GrpcStatus.OK, GrpcStatus.UNIMPLEMENTED));
        agent.failuresLeft = Integer.MAX_VALUE;
        client = new X509SvidCachingClientImpl(agent, FAST);
        assertThatThrownBy(() -> initialize(client, Duration.ofMinutes(5)).await().atMost(TIMEOUT))
                .isInstanceOf(CompletionException.class)
                .cause().isInstanceOf(SpiffeConnectionException.class)
                .hasMessageContaining("rejected the X.509-SVID request")
                .cause().isSameAs(agent.failure);
        assertThat(agent.fetches).hasValue(1);
    }

    @Test
    void longPeriodRetriesAFailedTickOnce() {
        StubClient agent = new StubClient(document(leaf));
        client = start(agent, FAST, Duration.ofSeconds(2));
        agent.document = document(rotatedLeaf);
        agent.failuresLeft = 1;
        // the tick at two seconds fails, its retry half a second later brings the rotated document before the next tick
        await().atMost(TIMEOUT).until(() -> client.getCurrentWorkloadCertificate() == agent.document);
        assertThat(agent.fetches).hasValue(3);
    }

    @Test
    void shortPeriodLeavesTheRetryToTheNextTick() {
        // a retry would block the ticks for its whole delay, so the document could not be replaced in time
        Timing slowRetry = new Timing(FAST.startupTimeout(), FAST.startupBackoff(), FAST.startupMaxBackoff(),
                Duration.ofSeconds(30), Duration.ofSeconds(1));
        StubClient agent = new StubClient(document(leaf));
        client = start(agent, slowRetry, Duration.ofMillis(300));
        agent.document = document(rotatedLeaf);
        agent.failuresLeft = 1;
        await().atMost(Duration.ofSeconds(3)).until(() -> client.getCurrentWorkloadCertificate() == agent.document);
    }

    @Test
    void unchangedDocumentIsKept() {
        StubClient agent = new StubClient(document(leaf));
        client = start(agent, FAST, Duration.ofMillis(100));
        WorkloadCertificateDocument first = client.getCurrentWorkloadCertificate();
        agent.document = document(leaf); // another instance with the same certificates
        await().atMost(TIMEOUT).until(() -> agent.fetches.get() >= 3);
        assertThat(client.getCurrentWorkloadCertificate()).isSameAs(first);
    }

    @Test
    void expiredDocumentIsDroppedWhenRead() {
        StubClient agent = new StubClient(document(expiredLeaf));
        client = start(agent, FAST, Duration.ofMinutes(5));
        assertThat(client.getCurrentWorkloadCertificate()).isNull();
    }

    @Test
    void deniedWorkloadDropsTheDocument() {
        StubClient agent = new StubClient(document(leaf));
        client = start(agent, FAST, Duration.ofMillis(100));
        agent.failure = new SpiffeAuthorizationException("PERMISSION_DENIED: no identity issued");
        agent.failuresLeft = Integer.MAX_VALUE;
        await().atMost(TIMEOUT).until(() -> client.getCurrentWorkloadCertificate() == null);
    }

    @Test
    void stopCancelsTheTicksAndCloseDropsTheDocument() {
        StubClient agent = new StubClient(document(leaf));
        client = start(agent, FAST, Duration.ofMillis(100));
        assertThat(stops).hasSize(1);
        stops.forEach(Runnable::run);
        client.close();
        int fetches = agent.fetches.get();
        assertThat(client.getCurrentWorkloadCertificate()).isNull();
        await().pollDelay(Duration.ofMillis(500)).until(() -> true);
        assertThat(agent.fetches).hasValue(fetches);
    }

    @Test
    void refreshPeriodMustBePositive() {
        client = new X509SvidCachingClientImpl(new StubClient(document(leaf)), FAST);
        assertThatThrownBy(() -> initialize(client, Duration.ZERO).await().atMost(TIMEOUT))
                .isInstanceOf(ConfigurationException.class);
    }

    private X509SvidCachingClientImpl start(StubClient agent, Timing timing, Duration refreshPeriod) {
        X509SvidCachingClientImpl client = new X509SvidCachingClientImpl(agent, timing);
        initialize(client, refreshPeriod).await().atMost(TIMEOUT);
        return client;
    }

    private Uni<WorkloadCertificateDocument> initialize(X509SvidCachingClientImpl client, Duration refreshPeriod) {
        return Uni.createFrom().emitter(
                emitter -> client.initialize(vertx, refreshPeriod, stops::add, emitter::fail, emitter::complete));
    }

    private static X509Certificate certificate(KeyPair keyPair, Duration validity) throws Exception {
        return CertificateUtils.generateCertificate(keyPair, "SPIFFE Workload", List.of("URI:" + SPIFFE_ID), validity,
                null, KeyAlgorithm.EC_P256);
    }

    private static WorkloadCertificateDocument document(X509Certificate leaf) {
        return new Document(SPIFFE_ID, new Chain(List.of(leaf), key), new Bundle(List.of(leaf)));
    }

    private record Document(String subject, WorkloadCertificateChain certificateChain, WorkloadTrustBundle trustBundle)
            implements
                WorkloadCertificateDocument {
    }

    private record Chain(List<X509Certificate> chain, PrivateKey privateKey) implements WorkloadCertificateChain {

        @Override
        public List<String> chainPem() {
            return List.of();
        }

        @Override
        public String privateKeyPem() {
            return "";
        }
    }

    private record Bundle(List<X509Certificate> certificates) implements WorkloadTrustBundle {

        @Override
        public List<String> certificatesPem() {
            return List.of();
        }
    }

    // the fake agent: fails the next fetches, holds them pending, or answers with the current document
    private static final class StubClient implements X509SvidClient {

        final AtomicInteger fetches = new AtomicInteger();
        final List<UniEmitter<? super WorkloadCertificateDocument>> pending = new CopyOnWriteArrayList<>();
        volatile WorkloadCertificateDocument document;
        volatile Throwable failure = new SpiffeConnectionException("UNAVAILABLE: agent not ready");
        volatile int failuresLeft;
        volatile boolean hold;

        StubClient(WorkloadCertificateDocument document) {
            this.document = document;
        }

        @Override
        public Uni<WorkloadCertificateDocument> getWorkloadCertificate() {
            return Uni.createFrom().emitter(emitter -> {
                fetches.incrementAndGet();
                if (hold) {
                    pending.add(emitter);
                } else if (failuresLeft-- > 0) {
                    emitter.fail(failure);
                } else {
                    emitter.complete(document);
                }
            });
        }

        void release() {
            for (UniEmitter<? super WorkloadCertificateDocument> emitter : pending) {
                emitter.complete(document);
            }
            pending.clear();
        }
    }
}
