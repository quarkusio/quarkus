package io.quarkus.deployment.dev;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
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
}
