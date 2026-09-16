package io.quarkus.gradle.application.internal.remotedev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.deployment.dev.remotedev.RemoteDevPackageClient;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageClientResult;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageDiff;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageReconnectListener;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageSnapshot;

class QuarkusApplicationRemoteDevDeploymentHandleTest {

    @TempDir
    Path directory;

    @Test
    void handleOwnsAndClosesOneSession() throws Exception {
        QuarkusApplicationRemoteDevDeploymentHandle handle = handle();
        handle.start(null);
        QuarkusApplicationRemoteDevSession session = handle.session();
        assertThat(handle.session()).isSameAs(session);
        FakeClient client = new FakeClient();
        Path packageRoot = Files.createDirectory(directory.resolve("package"));
        session.deliver(RemoteDevPackageSnapshot.capture(packageRoot), packageRoot,
                directory.resolve("snapshot.tsv"), () -> client);

        handle.stop();

        assertThat(client.closed).isTrue();
        assertThat(handle.isRunning()).isFalse();
        assertThat(directory.resolve("closed.txt")).hasContent("closed\n");
        assertThatThrownBy(handle::session)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not running");
        handle.stop();
        assertThat(client.closeCount).isEqualTo(1);
    }

    @Test
    void stoppedHandleCannotBeRestartedOrCreateSession() {
        QuarkusApplicationRemoteDevDeploymentHandle handle = handle();
        handle.stop();

        assertThatThrownBy(() -> handle.start(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be restarted");
        assertThatThrownBy(handle::session)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void stopRacingSessionCreationCannotLeaveAnOpenSession() throws Exception {
        QuarkusApplicationRemoteDevDeploymentHandle handle = handle();
        handle.start(null);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<QuarkusApplicationRemoteDevSession> acquisition = executor.submit(() -> {
                ready.countDown();
                start.await();
                return handle.session();
            });
            Future<?> stop = executor.submit(() -> {
                ready.countDown();
                start.await();
                handle.stop();
                return null;
            });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            stop.get(5, TimeUnit.SECONDS);

            try {
                QuarkusApplicationRemoteDevSession session = acquisition.get(5, TimeUnit.SECONDS);
                assertThatThrownBy(session::nextSequence)
                        .isInstanceOf(IOException.class)
                        .hasMessageContaining("closed");
            } catch (ExecutionException e) {
                assertThat(e).hasCauseInstanceOf(IllegalStateException.class);
            }
            assertThat(handle.isRunning()).isFalse();
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void stopPreservesClientAndCloseReceiptFailures() throws Exception {
        Path closeReceiptParent = directory.resolve("not-a-directory");
        Files.writeString(closeReceiptParent, "file");
        QuarkusApplicationRemoteDevDeploymentHandle handle = new QuarkusApplicationRemoteDevDeploymentHandle(
                closeReceiptParent.resolve("closed.txt"),
                directory.resolve("reconnect.trigger"),
                UUID.randomUUID().toString());
        handle.start(null);
        FakeClient client = new FakeClient();
        client.closeFailure = true;
        Path packageRoot = Files.createDirectory(directory.resolve("failure-package"));
        handle.session().deliver(RemoteDevPackageSnapshot.capture(packageRoot), packageRoot,
                directory.resolve("failure-snapshot.tsv"), () -> client);

        assertThatThrownBy(handle::stop)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to stop")
                .cause()
                .isInstanceOf(IOException.class)
                .hasMessageContaining("client close")
                .satisfies(cause -> assertThat(cause.getSuppressed()).hasSize(1));
    }

    private QuarkusApplicationRemoteDevDeploymentHandle handle() {
        return new QuarkusApplicationRemoteDevDeploymentHandle(
                directory.resolve("closed.txt"),
                directory.resolve("reconnect.trigger"),
                UUID.randomUUID().toString());
    }

    private static final class FakeClient implements RemoteDevPackageClient {
        private boolean closed;
        private int closeCount;
        private boolean closeFailure;

        @Override
        public RemoteDevPackageClientResult connect(Map<String, String> localHashes) {
            return RemoteDevPackageClientResult.connected(Set.of());
        }

        @Override
        public RemoteDevPackageClientResult send(RemoteDevPackageDiff diff) {
            return RemoteDevPackageClientResult.sent(diff.changed().size(), diff.deleted().size());
        }

        @Override
        public void startChangePolling(RemoteDevPackageReconnectListener listener) {
        }

        @Override
        public void close() throws IOException {
            closed = true;
            closeCount++;
            if (closeFailure) {
                throw new IOException("client close");
            }
        }
    }
}
