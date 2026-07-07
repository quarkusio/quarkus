package io.quarkus.deployment.dev;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BuildOutputChangesTcpClientTest {

    @TempDir
    Path directory;

    @Test
    void connectsToServerAndDispatchesAuthenticatedMessage() throws Exception {
        var received = new AtomicReference<BuildOutputChanges>();
        var delivered = new CountDownLatch(1);
        try (var server = new BuildOutputChangesServer("secret");
                var ignore = new BuildOutputChangesTcpClient(server.address(), "secret", changes -> {
                    received.set(changes);
                    delivered.countDown();
                    return BuildOutputChangesApplyStatus.APPLIED;
                })) {
            assertThat(server.awaitAuthenticated()).isTrue();
            assertThat(server.send(changes(1))).isEqualTo(BuildOutputChangesApplyStatus.APPLIED);

            assertThat(delivered.await(60, SECONDS)).isTrue();
            assertThat(received.get().sequence()).isEqualTo(1);
        }
    }

    @Test
    void sendsTokenInHelloAndServerCanRejectIt() throws Exception {
        var delivered = new CountDownLatch(1);
        try (var server = new BuildOutputChangesServer("secret");
                var ignore = new BuildOutputChangesTcpClient(server.address(), "wrong", ignored -> {
                    delivered.countDown();
                    return BuildOutputChangesApplyStatus.APPLIED;
                })) {
            assertThat(server.awaitRejected()).isTrue();
        }
        assertThat(delivered.await(0, SECONDS)).isFalse();
    }

    @Test
    void closeStopsClientReader() throws Exception {
        try (var server = new BuildOutputChangesServer("secret")) {
            var client = new BuildOutputChangesTcpClient(server.address(), "secret", ignored -> {
                return BuildOutputChangesApplyStatus.APPLIED;
            });

            assertThat(server.awaitAuthenticated()).isTrue();
            client.close();

            assertThat(server.awaitConnectionClosed()).isTrue();
        }
    }

    @Test
    void publishesOnlyTheNewestAcceptedStateGeneration() throws Exception {
        try (var server = new BuildOutputChangesServer("secret");
                var client = new BuildOutputChangesTcpClient(server.address(), "secret",
                        ignored -> BuildOutputChangesApplyStatus.APPLIED)) {
            assertThat(server.awaitAuthenticated()).isTrue();

            client.liveReloadStateChanged(new BuildOutputLiveReloadState(2, true));
            client.liveReloadStateChanged(new BuildOutputLiveReloadState(1, false));

            assertThat(server.readMessage())
                    .isEqualTo(new BuildOutputChangesProtocol.LiveReloadState(
                            new BuildOutputLiveReloadState(2, true)));
            assertThatThrownBy(server::readMessageWithShortTimeout)
                    .isInstanceOf(SocketTimeoutException.class);
        }
    }

    @Test
    void invalidServerMessageDirectionClosesClient() throws Exception {
        try (var server = new BuildOutputChangesServer("secret");
                var ignored = new BuildOutputChangesTcpClient(server.address(), "secret",
                        changes -> BuildOutputChangesApplyStatus.APPLIED)) {
            assertThat(server.awaitAuthenticated()).isTrue();

            server.sendPayload(BuildOutputChangesProtocol.encodeApplyResult(1,
                    BuildOutputChangesApplyStatus.APPLIED));

            assertThat(server.awaitConnectionClosed()).isTrue();
        }
    }

    @Test
    void consumerFailureReturnsNotAppliedWithoutCorruptingTheConnection() throws Exception {
        try (var server = new BuildOutputChangesServer("secret");
                var ignored = new BuildOutputChangesTcpClient(server.address(), "secret", changes -> {
                    if (changes.sequence() == 1) {
                        throw new IllegalStateException("expected consumer failure");
                    }
                    return BuildOutputChangesApplyStatus.APPLIED;
                })) {
            assertThat(server.awaitAuthenticated()).isTrue();

            assertThat(server.send(changes(1))).isEqualTo(BuildOutputChangesApplyStatus.NOT_APPLIED);
            assertThat(server.send(changes(2))).isEqualTo(BuildOutputChangesApplyStatus.APPLIED);
        }
    }

    private BuildOutputChanges changes(long sequence) {
        var classesRoot = directory.resolve("classes");
        return new BuildOutputChanges(sequence, BuildOutputChangeStatus.BUILD_SUCCEEDED,
                List.of(new BuildOutputPathChange(classesRoot, classesRoot.resolve("com/acme/Foo.class"),
                        BuildOutputChangeKind.MODIFIED)),
                null, null, null, null, null, false, false);
    }

    private static final class BuildOutputChangesServer implements Closeable {
        private final ServerSocket serverSocket;
        private final CountDownLatch authenticated = new CountDownLatch(1);
        private final CountDownLatch rejected = new CountDownLatch(1);
        private final String expectedToken;
        private volatile Socket socket;

        private BuildOutputChangesServer(String expectedToken) throws IOException {
            this.expectedToken = expectedToken;
            serverSocket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
            var acceptThread = new Thread(this::accept, "Build Output Changes Test Server");
            acceptThread.setDaemon(true);
            acceptThread.start();
        }

        InetSocketAddress address() {
            return new InetSocketAddress(InetAddress.getLoopbackAddress(), serverSocket.getLocalPort());
        }

        BuildOutputChangesApplyStatus send(BuildOutputChanges changes) throws Exception {
            assertThat(authenticated.await(60, SECONDS)).isTrue();
            BuildOutputChangesFrameCodec.write(socket.getOutputStream(),
                    BuildOutputChangesProtocol.encodeChanges(changes.sequence(), changes));
            var result = (BuildOutputChangesProtocol.ApplyResult) BuildOutputChangesProtocol.decode(
                    BuildOutputChangesFrameCodec.read(socket.getInputStream()));
            assertThat(result.requestId()).isEqualTo(changes.sequence());
            return result.status();
        }

        BuildOutputChangesProtocol.Message readMessage() throws Exception {
            assertThat(authenticated.await(60, SECONDS)).isTrue();
            return BuildOutputChangesProtocol.decode(BuildOutputChangesFrameCodec.read(socket.getInputStream()));
        }

        BuildOutputChangesProtocol.Message readMessageWithShortTimeout() throws Exception {
            socket.setSoTimeout(200);
            try {
                return readMessage();
            } finally {
                socket.setSoTimeout(0);
            }
        }

        void sendPayload(String payload) throws Exception {
            assertThat(authenticated.await(60, SECONDS)).isTrue();
            BuildOutputChangesFrameCodec.write(socket.getOutputStream(), payload);
        }

        boolean awaitAuthenticated() throws InterruptedException {
            return authenticated.await(60, SECONDS);
        }

        boolean awaitRejected() throws InterruptedException {
            return rejected.await(60, SECONDS);
        }

        boolean awaitConnectionClosed() throws InterruptedException {
            assertThat(authenticated.await(60, SECONDS)).isTrue();
            try {
                return socket.getInputStream().read() == -1;
            } catch (IOException e) {
                return true;
            }
        }

        @Override
        public void close() throws IOException {
            serverSocket.close();
            if (socket != null) {
                socket.close();
            }
        }

        private void accept() {
            try {
                socket = serverSocket.accept();
                String token = BuildOutputChangesProtocol.readHello(socket.getInputStream());
                if (!expectedToken.equals(token)) {
                    rejected.countDown();
                    socket.close();
                    return;
                }
                authenticated.countDown();
            } catch (IOException ignored) {
            }
        }
    }
}
