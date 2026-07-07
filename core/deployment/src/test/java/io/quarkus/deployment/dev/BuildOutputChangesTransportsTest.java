package io.quarkus.deployment.dev;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BuildOutputChangesTransportsTest {

    @TempDir
    Path directory;

    @Test
    void disabledTransportReturnsNoopCloseable() throws Exception {
        var delivered = new CountDownLatch(1);

        var closeable = BuildOutputChangesTransports.connect(
                DevModeContext.ExternalBuildOutputTransport.disabled(),
                ignored -> {
                    delivered.countDown();
                    return BuildOutputChangesApplyStatus.APPLIED;
                });

        closeable.close();
        assertThat(delivered.await(0, SECONDS)).isFalse();
    }

    @SuppressWarnings("resource")
    @Test
    void unsupportedTransportSchemeFailsFast() {
        assertThatThrownBy(() -> BuildOutputChangesTransports.connect(
                DevModeContext.ExternalBuildOutputTransport.of(URI.create("file:///tmp/quarkus-build-output"), "secret"),
                ignored -> {
                    return BuildOutputChangesApplyStatus.APPLIED;
                }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported external build output transport URI scheme: file");
    }

    @SuppressWarnings("resource")
    @Test
    void missingTransportSchemeFailsFast() {
        assertThatThrownBy(() -> BuildOutputChangesTransports.connect(
                DevModeContext.ExternalBuildOutputTransport.of(URI.create("//127.0.0.1:12345"), "secret"),
                ignored -> {
                    return BuildOutputChangesApplyStatus.APPLIED;
                }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("External build output transport URI scheme is required");
    }

    @SuppressWarnings("resource")
    @Test
    void missingTcpPortFailsFast() {
        assertThatThrownBy(() -> BuildOutputChangesTransports.connect(
                DevModeContext.ExternalBuildOutputTransport.of(URI.create("tcp://127.0.0.1"), "secret"),
                ignored -> {
                    return BuildOutputChangesApplyStatus.APPLIED;
                }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("External build output TCP URI port is required");
    }

    @SuppressWarnings("resource")
    @Test
    void nonLoopbackTcpHostFailsFast() {
        assertThatThrownBy(() -> BuildOutputChangesTransports.connect(
                DevModeContext.ExternalBuildOutputTransport.of(URI.create("tcp://192.0.2.10:12345"), "secret"),
                ignored -> {
                    return BuildOutputChangesApplyStatus.APPLIED;
                }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("External build output TCP URI host must be a loopback address");
    }

    @SuppressWarnings("resource")
    @Test
    void blankTokenFailsFast() {
        assertThatThrownBy(() -> BuildOutputChangesTransports.connect(
                DevModeContext.ExternalBuildOutputTransport.of(URI.create("tcp://127.0.0.1:12345"), " "),
                ignored -> {
                    return BuildOutputChangesApplyStatus.APPLIED;
                }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("External build output transport token must not be blank");
    }

    @Test
    void tcpServerExposesEnabledTransportMetadata() throws Exception {
        try (var server = BuildOutputChangesTransports.createTcpServer()) {
            var transport = server.transport();

            assertThat(transport.isEnabled()).isTrue();
            assertThat(transport.getUri()).hasValueSatisfying(uri -> {
                assertThat(uri.getScheme()).isEqualTo("tcp");
                assertThat(uri.getHost()).isEqualTo("127.0.0.1");
                assertThat(uri.getPort()).isPositive();
            });
            assertThat(transport.getToken()).hasValueSatisfying(token -> assertThat(token).isNotBlank());
        }
    }

    @Test
    void tcpServerReturnsIndependentTransportMetadata() throws Exception {
        try (var server = BuildOutputChangesTransports.createTcpServer()) {
            var transport = server.transport();
            transport.setUri(null);
            transport.setToken(null);

            assertThat(server.transport().isEnabled()).isTrue();
            assertThat(server.transport().getToken()).hasValueSatisfying(token -> assertThat(token).isNotBlank());
        }
    }

    @Test
    void tcpServerReportsExplicitCloseAsExpectedTermination() throws Exception {
        var server = BuildOutputChangesTransports.createTcpServer();

        server.close();

        assertThat(server.termination().toCompletableFuture().get(5, SECONDS)).isNull();
    }

    @Test
    void tcpServerReportsLostAuthenticatedConnectionAsUnexpectedTermination() throws Exception {
        var server = BuildOutputChangesTransports.createTcpServer();
        try (server;
                var connection = BuildOutputChangesTransports.connect(server.transport(),
                        ignored -> BuildOutputChangesApplyStatus.APPLIED)) {
            connection.close();

            assertThatThrownBy(() -> server.termination().toCompletableFuture().get(5, SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .cause()
                    .isInstanceOf(IOException.class)
                    .hasMessage("External build output transport terminated unexpectedly");
        }
    }

    @Test
    void tcpTransportConnectsAndDispatchesAuthenticatedMessages() throws Exception {
        var received = new AtomicReference<BuildOutputChanges>();
        var delivered = new CountDownLatch(1);
        try (var server = BuildOutputChangesTransports.createTcpServer();
                var ignored = BuildOutputChangesTransports.connect(server.transport(),
                        changes -> {
                            received.set(changes);
                            delivered.countDown();
                            return BuildOutputChangesApplyStatus.APPLIED;
                        })) {
            assertThat(server.send(changes(1))).isEqualTo(BuildOutputChangesApplyStatus.APPLIED);

            assertThat(delivered.await(60, SECONDS)).isTrue();
            assertThat(received.get().sequence()).isEqualTo(1);
        }
    }

    @Test
    void tcpTransportReturnsNotAppliedWhenConsumerDoesNotApplyMessage() throws Exception {
        var received = new AtomicReference<BuildOutputChanges>();
        var delivered = new CountDownLatch(1);
        try (var server = BuildOutputChangesTransports.createTcpServer();
                var ignored = BuildOutputChangesTransports.connect(server.transport(),
                        changes -> {
                            received.set(changes);
                            delivered.countDown();
                            return BuildOutputChangesApplyStatus.NOT_APPLIED;
                        })) {
            assertThat(server.send(changes(1))).isEqualTo(BuildOutputChangesApplyStatus.NOT_APPLIED);

            assertThat(delivered.await(60, SECONDS)).isTrue();
            assertThat(received.get().sequence()).isEqualTo(1);
        }
    }

    @Test
    void tcpTransportDispatchesLiveReloadStateWithoutSendInFlight() throws Exception {
        var received = new AtomicReference<BuildOutputLiveReloadState>();
        var delivered = new CountDownLatch(1);
        try (var server = BuildOutputChangesTransports.createTcpServer(state -> {
            received.set(state);
            delivered.countDown();
        });
                var connection = BuildOutputChangesTransports.connect(server.transport(),
                        ignored -> BuildOutputChangesApplyStatus.APPLIED)) {
            connection.liveReloadStateChanged(new BuildOutputLiveReloadState(1, false));

            assertThat(delivered.await(60, SECONDS)).isTrue();
            assertThat(received.get()).isEqualTo(new BuildOutputLiveReloadState(1, false));
        }
    }

    @Test
    void tcpTransportDispatchesStateWhileARequestIsInFlight() throws Exception {
        var applyStarted = new CountDownLatch(1);
        var completeApply = new CountDownLatch(1);
        var stateDelivered = new CountDownLatch(1);
        try (var server = BuildOutputChangesTransports.createTcpServer(ignored -> stateDelivered.countDown());
                var connection = BuildOutputChangesTransports.connect(server.transport(),
                        ignored -> {
                            applyStarted.countDown();
                            await(completeApply);
                            return BuildOutputChangesApplyStatus.APPLIED;
                        })) {
            CompletableFuture<BuildOutputChangesApplyStatus> send = sendAsync(server, changes(1));
            assertThat(applyStarted.await(5, SECONDS)).isTrue();

            connection.liveReloadStateChanged(new BuildOutputLiveReloadState(1, false));
            assertThat(stateDelivered.await(5, SECONDS)).isTrue();
            completeApply.countDown();

            assertThat(send.get(5, SECONDS)).isEqualTo(BuildOutputChangesApplyStatus.APPLIED);
        } finally {
            completeApply.countDown();
        }
    }

    @Test
    void tcpTransportSerializesConcurrentSendsAndCorrelatesTheirResults() throws Exception {
        var firstApplyStarted = new CountDownLatch(1);
        var completeFirstApply = new CountDownLatch(1);
        var sequences = new CopyOnWriteArrayList<Long>();
        try (var server = BuildOutputChangesTransports.createTcpServer();
                var ignored = BuildOutputChangesTransports.connect(server.transport(),
                        changes -> {
                            sequences.add(changes.sequence());
                            if (changes.sequence() == 1) {
                                firstApplyStarted.countDown();
                                await(completeFirstApply);
                            }
                            return changes.sequence() == 1
                                    ? BuildOutputChangesApplyStatus.NOT_APPLIED
                                    : BuildOutputChangesApplyStatus.APPLIED;
                        })) {
            CompletableFuture<BuildOutputChangesApplyStatus> first = sendAsync(server, changes(1));
            assertThat(firstApplyStarted.await(5, SECONDS)).isTrue();
            CompletableFuture<BuildOutputChangesApplyStatus> second = sendAsync(server, changes(2));
            assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);

            completeFirstApply.countDown();

            assertThat(first.get(5, SECONDS)).isEqualTo(BuildOutputChangesApplyStatus.NOT_APPLIED);
            assertThat(second.get(5, SECONDS)).isEqualTo(BuildOutputChangesApplyStatus.APPLIED);
            assertThat(sequences).containsExactly(1L, 2L);
        } finally {
            completeFirstApply.countDown();
        }
    }

    @Test
    void tcpTransportIgnoresStaleStateAndSurvivesCallbackFailure() throws Exception {
        var callbacks = new CopyOnWriteArrayList<BuildOutputLiveReloadState>();
        var firstDelivered = new CountDownLatch(1);
        var secondDelivered = new CountDownLatch(1);
        try (var server = BuildOutputChangesTransports.createTcpServer(state -> {
            callbacks.add(state);
            if (state.generation() == 2) {
                firstDelivered.countDown();
                throw new IllegalStateException("expected test callback failure");
            }
            secondDelivered.countDown();
        });
                var connection = BuildOutputChangesTransports.connect(server.transport(),
                        ignored -> BuildOutputChangesApplyStatus.APPLIED)) {
            connection.liveReloadStateChanged(new BuildOutputLiveReloadState(2, false));
            assertThat(firstDelivered.await(5, SECONDS)).isTrue();
            connection.liveReloadStateChanged(new BuildOutputLiveReloadState(1, true));
            connection.liveReloadStateChanged(new BuildOutputLiveReloadState(3, true));

            assertThat(secondDelivered.await(5, SECONDS)).isTrue();
            assertThat(callbacks).containsExactly(
                    new BuildOutputLiveReloadState(2, false),
                    new BuildOutputLiveReloadState(3, true));
            assertThat(server.send(changes(1))).isEqualTo(BuildOutputChangesApplyStatus.APPLIED);
        }
    }

    @Test
    void tcpServerTimesOutARequestAndClosesTheConnection() throws Exception {
        try (var server = new BuildOutputChangesTcpServer(ignored -> {
        }, Duration.ofMillis(200), Duration.ofSeconds(1), 0);
                var socket = authenticatedSocket(server)) {
            CompletableFuture<BuildOutputChangesApplyStatus> send = sendAsync(server, changes(1));
            assertThat(BuildOutputChangesProtocol.decode(
                    BuildOutputChangesFrameCodec.read(socket.getInputStream())))
                    .isInstanceOf(BuildOutputChangesProtocol.Changes.class);

            assertThatThrownBy(() -> send.get(5, SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .cause()
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("Timed out");
            assertConnectionClosed(socket);
        }
    }

    @Test
    void tcpServerRejectsUnknownRequestIdAndClosesTheConnection() throws Exception {
        try (var server = new BuildOutputChangesTcpServer(ignored -> {
        }, Duration.ofSeconds(5), Duration.ofSeconds(1), 0);
                var socket = authenticatedSocket(server)) {
            CompletableFuture<BuildOutputChangesApplyStatus> send = sendAsync(server, changes(1));
            var request = (BuildOutputChangesProtocol.Changes) BuildOutputChangesProtocol.decode(
                    BuildOutputChangesFrameCodec.read(socket.getInputStream()));
            BuildOutputChangesFrameCodec.write(socket.getOutputStream(),
                    BuildOutputChangesProtocol.encodeApplyResult(request.requestId() + 1,
                            BuildOutputChangesApplyStatus.APPLIED));

            assertThatThrownBy(() -> send.get(5, SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .cause()
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("request ID");
            assertConnectionClosed(socket);
        }
    }

    @Test
    void tcpServerClosesAfterUsingTheLastRequestId() throws Exception {
        try (var server = new BuildOutputChangesTcpServer(ignored -> {
        }, Duration.ofSeconds(5), Duration.ofSeconds(1), Long.MAX_VALUE);
                var ignored = BuildOutputChangesTransports.connect(server.transport(),
                        changes -> BuildOutputChangesApplyStatus.APPLIED)) {
            assertThat(server.send(changes(1))).isEqualTo(BuildOutputChangesApplyStatus.APPLIED);
            assertThatThrownBy(() -> server.send(changes(2)))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("request IDs are exhausted");
        }
    }

    @Test
    void tcpTransportPreservesRebaselineDeliveryKind() throws Exception {
        var received = new AtomicReference<BuildOutputChanges>();
        try (var server = BuildOutputChangesTransports.createTcpServer();
                var ignored = BuildOutputChangesTransports.connect(server.transport(),
                        changes -> {
                            received.set(changes);
                            return BuildOutputChangesApplyStatus.APPLIED;
                        })) {
            var rebaseline = new BuildOutputChanges(3, BuildOutputChangeStatus.BUILD_SUCCEEDED,
                    BuildOutputFailureKind.NONE, null, null, null, null, null, null, false, true,
                    BuildOutputChangesDeliveryKind.REBASELINE);

            assertThat(server.send(rebaseline)).isEqualTo(BuildOutputChangesApplyStatus.APPLIED);
            assertThat(received.get().deliveryKind()).isEqualTo(BuildOutputChangesDeliveryKind.REBASELINE);
        }
    }

    @Test
    void tcpServerKeepsListeningAfterRejectedClient() throws Exception {
        var rejectedDelivered = new CountDownLatch(1);
        var received = new AtomicReference<BuildOutputChanges>();
        var delivered = new CountDownLatch(1);
        try (var server = BuildOutputChangesTransports.createTcpServer()) {
            var rejectedTransport = DevModeContext.ExternalBuildOutputTransport.of(
                    server.transport().getUri().orElseThrow(), "wrong-token");
            try (var ignored = BuildOutputChangesTransports.connect(rejectedTransport,
                    changes -> {
                        rejectedDelivered.countDown();
                        return BuildOutputChangesApplyStatus.APPLIED;
                    })) {
            }

            try (var ignored = BuildOutputChangesTransports.connect(server.transport(),
                    changes -> {
                        received.set(changes);
                        delivered.countDown();
                        return BuildOutputChangesApplyStatus.APPLIED;
                    })) {
                assertThat(server.send(changes(2))).isEqualTo(BuildOutputChangesApplyStatus.APPLIED);

                assertThat(rejectedDelivered.await(0, SECONDS)).isFalse();
                assertThat(delivered.await(60, SECONDS)).isTrue();
                assertThat(received.get().sequence()).isEqualTo(2);
            }
        }
    }

    private BuildOutputChanges changes(long sequence) {
        var classesRoot = directory.resolve("classes");
        return new BuildOutputChanges(sequence, BuildOutputChangeStatus.BUILD_SUCCEEDED,
                List.of(new BuildOutputPathChange(classesRoot, classesRoot.resolve("com/acme/Foo.class"),
                        BuildOutputChangeKind.MODIFIED)),
                null, null, null, null, null, false, false);
    }

    private static CompletableFuture<BuildOutputChangesApplyStatus> sendAsync(
            BuildOutputChangesServer server, BuildOutputChanges changes) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return server.send(changes);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    private static Socket authenticatedSocket(BuildOutputChangesServer server) throws Exception {
        DevModeContext.ExternalBuildOutputTransport transport = server.transport();
        URI uri = transport.getUri().orElseThrow();
        var socket = new Socket(uri.getHost(), uri.getPort());
        BuildOutputChangesProtocol.writeHello(socket.getOutputStream(), transport.getToken().orElseThrow());
        return socket;
    }

    private static void assertConnectionClosed(Socket socket) {
        try {
            assertThat(socket.getInputStream().read()).isEqualTo(-1);
        } catch (IOException expected) {
            assertThat(expected).isNotNull();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, SECONDS)) {
                throw new IllegalStateException("Timed out waiting for test latch");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for test latch", e);
        }
    }
}
