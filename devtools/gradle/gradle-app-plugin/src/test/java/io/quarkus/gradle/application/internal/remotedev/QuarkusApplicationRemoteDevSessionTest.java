package io.quarkus.gradle.application.internal.remotedev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.deployment.dev.remotedev.RemoteDevPackageChange;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageClient;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageClientOutcome;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageClientResult;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageDiff;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageReconnectListener;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageSnapshot;

class QuarkusApplicationRemoteDevSessionTest {

    @TempDir
    Path directory;

    @Test
    void initialConnectSendsOnlyServerRequestedFilesAndStoresBaseline() throws Exception {
        Path packageRoot = createPackage("app.properties", "ignored",
                "lib/deployment/appmodel.dat", "requested");
        RemoteDevPackageSnapshot snapshot = RemoteDevPackageSnapshot.capture(packageRoot);
        FakeClient client = new FakeClient();
        client.request(Set.of("lib/deployment/appmodel.dat"));
        QuarkusApplicationRemoteDevSession session = new QuarkusApplicationRemoteDevSession();

        QuarkusApplicationRemoteDevSession.DeliveryResult result = session.deliver(snapshot, packageRoot,
                directory.resolve("snapshot.tsv"), () -> client);

        assertThat(result.outcome()).isEqualTo("CONNECTED");
        assertThat(result.changed()).isEqualTo(1);
        assertThat(client.connectedHashes).containsKeys("app.properties", "lib/deployment/appmodel.dat");
        assertThat(client.sentChanges).containsExactly(List.of("lib/deployment/appmodel.dat"));
        assertThat(client.pollingStarted).isTrue();

        QuarkusApplicationRemoteDevSession.DeliveryResult second = session.deliver(snapshot, packageRoot,
                directory.resolve("snapshot.tsv"), () -> client);

        assertThat(second.outcome()).isEqualTo("NO_CHANGES");
        assertThat(client.sentChanges).hasSize(1);
    }

    @Test
    void staleChangedFileReconnectCanSupplyDifferentUnchangedFile() throws Exception {
        Path packageRoot = createPackage("app/changed.jar", "one", "app/unchanged.jar", "stable");
        Path trigger = directory.resolve("reconnect.trigger");
        FakeClient client = new FakeClient();
        client.request(Set.of());
        QuarkusApplicationRemoteDevSession session = new QuarkusApplicationRemoteDevSession(
                trigger, UUID.randomUUID().toString());
        RemoteDevPackageSnapshot first = RemoteDevPackageSnapshot.capture(packageRoot);
        session.deliver(first, packageRoot, directory.resolve("snapshot.tsv"), () -> client);

        Files.writeString(packageRoot.resolve("app/changed.jar"), "two");
        RemoteDevPackageSnapshot second = RemoteDevPackageSnapshot.capture(packageRoot);
        client.sendOutcome(RemoteDevPackageClientOutcome.RECONNECT_REQUIRED);
        client.request(Set.of("app/unchanged.jar"));

        QuarkusApplicationRemoteDevSession.DeliveryResult result = session.deliver(second, packageRoot,
                directory.resolve("snapshot.tsv"), () -> client);

        assertThat(result.outcome()).isEqualTo("SENT");
        assertThat(result.requestedBatches()).singleElement()
                .satisfies(batch -> assertThat(batch.changed())
                        .extracting(RemoteDevPackageChange::relativePath)
                        .containsExactly("app/unchanged.jar"));
        assertThat(client.sentChanges).containsExactly(
                List.of("app/changed.jar"),
                List.of("app/unchanged.jar"));
        assertThat(client.connects).isEqualTo(2);
        assertThat(Files.readString(trigger)).matches("epoch=[0-9a-f-]+\\ngeneration=1\\n");

        QuarkusApplicationRemoteDevSession.DeliveryResult extra = session.deliver(second, packageRoot,
                directory.resolve("snapshot.tsv"), () -> client);
        assertThat(extra.outcome()).isEqualTo("NO_CHANGES");
        assertThat(client.connects).isEqualTo(2);
    }

    @Test
    void asynchronousStaleNotificationTriggersNoEditReconciliation() throws Exception {
        Path packageRoot = createPackage("app/unchanged.jar", "stable");
        Path trigger = directory.resolve("reconnect.trigger");
        FakeClient client = new FakeClient();
        client.request(Set.of());
        QuarkusApplicationRemoteDevSession session = new QuarkusApplicationRemoteDevSession(
                trigger, UUID.randomUUID().toString());
        RemoteDevPackageSnapshot snapshot = RemoteDevPackageSnapshot.capture(packageRoot);
        session.deliver(snapshot, packageRoot, directory.resolve("snapshot.tsv"), () -> client);

        client.request(Set.of("app/unchanged.jar"));
        client.reconnectListener.reconnectRequired();

        QuarkusApplicationRemoteDevSession.DeliveryResult result = session.deliver(snapshot, packageRoot,
                directory.resolve("snapshot.tsv"), () -> client);

        assertThat(result.outcome()).isEqualTo("CONNECTED");
        assertThat(result.changed()).isEqualTo(1);
        assertThat(client.sentChanges).containsExactly(List.of("app/unchanged.jar"));
        assertThat(Files.readString(trigger)).matches("epoch=[0-9a-f-]+\\ngeneration=1\\n");
    }

    @Test
    void asynchronousReconnectIncludesSimultaneousLocalChanges() throws Exception {
        Path packageRoot = createPackage("app/changed.jar", "one", "app/unchanged.jar", "stable");
        FakeClient client = new FakeClient();
        client.request(Set.of());
        QuarkusApplicationRemoteDevSession session = new QuarkusApplicationRemoteDevSession();
        RemoteDevPackageSnapshot first = RemoteDevPackageSnapshot.capture(packageRoot);
        session.deliver(first, packageRoot, directory.resolve("snapshot.tsv"), () -> client);

        Files.writeString(packageRoot.resolve("app/changed.jar"), "two");
        RemoteDevPackageSnapshot current = RemoteDevPackageSnapshot.capture(packageRoot);
        client.request(Set.of("app/changed.jar", "app/unchanged.jar"));
        client.reconnectListener.reconnectRequired();

        QuarkusApplicationRemoteDevSession.DeliveryResult result = session.deliver(current, packageRoot,
                directory.resolve("snapshot.tsv"), () -> client);

        assertThat(result.outcome()).isEqualTo("CONNECTED");
        assertThat(client.sentChanges).containsExactly(List.of("app/changed.jar", "app/unchanged.jar"));
    }

    @Test
    void failedTriggerPublicationRetriesSameGenerationAndSuccessfulNotificationsCoalesce() throws Exception {
        Path packageRoot = createPackage("app/unchanged.jar", "stable");
        Path triggerParent = directory.resolve("blocked-trigger-parent");
        Files.writeString(triggerParent, "not a directory");
        Path trigger = triggerParent.resolve("reconnect.trigger");
        FakeClient client = new FakeClient();
        client.request(Set.of());
        QuarkusApplicationRemoteDevSession session = new QuarkusApplicationRemoteDevSession(
                trigger, UUID.randomUUID().toString());
        RemoteDevPackageSnapshot snapshot = RemoteDevPackageSnapshot.capture(packageRoot);
        session.deliver(snapshot, packageRoot, directory.resolve("snapshot.tsv"), () -> client);

        assertThatThrownBy(client.reconnectListener::reconnectRequired)
                .isInstanceOf(IOException.class);
        Files.delete(triggerParent);
        client.reconnectListener.reconnectRequired();
        String published = Files.readString(trigger);
        client.reconnectListener.reconnectRequired();

        assertThat(published).matches("epoch=[0-9a-f-]+\\ngeneration=1\\n");
        assertThat(trigger).hasContent(published);
    }

    @Test
    void closeDuringDeliveryPreventsSnapshotCommitAndLaterCallbackPublication() throws Exception {
        Path packageRoot = createPackage("lib/application.jar", "one");
        Path snapshotFile = directory.resolve("snapshot.tsv");
        Path trigger = directory.resolve("reconnect.trigger");
        FakeClient client = new FakeClient();
        client.request(Set.of());
        QuarkusApplicationRemoteDevSession session = new QuarkusApplicationRemoteDevSession(
                trigger, UUID.randomUUID().toString());
        session.deliver(RemoteDevPackageSnapshot.capture(packageRoot), packageRoot, snapshotFile, () -> client);
        String originalSnapshot = Files.readString(snapshotFile);

        Files.writeString(packageRoot.resolve("lib/application.jar"), "two");
        RemoteDevPackageSnapshot changed = RemoteDevPackageSnapshot.capture(packageRoot);
        client.blockSendsUntilClose = true;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> delivery = executor.submit(() -> session.deliver(changed, packageRoot, snapshotFile, () -> client));
            assertThat(client.sendStarted.await(5, TimeUnit.SECONDS)).isTrue();

            session.close();

            assertThatThrownBy(() -> delivery.get(5, TimeUnit.SECONDS))
                    .hasRootCauseInstanceOf(IOException.class);
            assertThat(snapshotFile).hasContent(originalSnapshot);
            client.reconnectListener.reconnectRequired();
            assertThat(trigger).doesNotExist();
        } finally {
            client.releaseSend.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void failedIncrementalDeliveryDoesNotAdvanceBaseline() throws Exception {
        Path packageRoot = createPackage("lib/application.jar", "one");
        RemoteDevPackageSnapshot first = RemoteDevPackageSnapshot.capture(packageRoot);
        FakeClient client = new FakeClient();
        client.request(Set.of());
        QuarkusApplicationRemoteDevSession session = new QuarkusApplicationRemoteDevSession();
        session.deliver(first, packageRoot, directory.resolve("snapshot.tsv"), () -> client);

        Files.writeString(packageRoot.resolve("lib/application.jar"), "two");
        RemoteDevPackageSnapshot second = RemoteDevPackageSnapshot.capture(packageRoot);
        client.failSends = true;

        assertThatThrownBy(() -> session.deliver(second, packageRoot, directory.resolve("snapshot.tsv"), () -> client))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("boom");

        client.failSends = false;
        QuarkusApplicationRemoteDevSession.DeliveryResult result = session.deliver(second, packageRoot,
                directory.resolve("snapshot.tsv"), () -> client);

        assertThat(result.changed()).isEqualTo(1);
        assertThat(client.sentChanges).contains(List.of("lib/application.jar"));
    }

    @Test
    void boundsRepeatedApplicationModelReconciliation() throws Exception {
        Path packageRoot = createPackage("lib/deployment/appmodel.dat", "model");
        RemoteDevPackageSnapshot snapshot = RemoteDevPackageSnapshot.capture(packageRoot);
        FakeClient client = new FakeClient();
        for (int i = 0; i < 4; i++) {
            client.request(Set.of("lib/deployment/appmodel.dat"));
            client.sendOutcome(RemoteDevPackageClientOutcome.RECONNECT_REQUIRED);
        }
        QuarkusApplicationRemoteDevSession session = new QuarkusApplicationRemoteDevSession();

        assertThatThrownBy(() -> session.deliver(snapshot, packageRoot, directory.resolve("snapshot.tsv"), () -> client))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("did not stabilize after 4 connection attempts");
        assertThat(client.connects).isEqualTo(4);
        assertThat(directory.resolve("snapshot.tsv")).doesNotExist();
    }

    private Path createPackage(String... pathAndContent) throws IOException {
        Path packageRoot = Files.createDirectories(directory.resolve("package"));
        for (int i = 0; i < pathAndContent.length; i += 2) {
            Path file = packageRoot.resolve(pathAndContent[i]);
            Files.createDirectories(file.getParent());
            Files.writeString(file, pathAndContent[i + 1]);
        }
        return packageRoot;
    }

    private static final class FakeClient implements RemoteDevPackageClient {

        private final Deque<Set<String>> requestedPaths = new ArrayDeque<>();
        private final Deque<RemoteDevPackageClientOutcome> sendOutcomes = new ArrayDeque<>();
        private final List<List<String>> sentChanges = new ArrayList<>();
        private Map<String, String> connectedHashes = Map.of();
        private RemoteDevPackageReconnectListener reconnectListener;
        private int connects;
        private boolean failSends;
        private boolean pollingStarted;
        private boolean closed;
        private boolean blockSendsUntilClose;
        private final CountDownLatch sendStarted = new CountDownLatch(1);
        private final CountDownLatch releaseSend = new CountDownLatch(1);

        void request(Set<String> requested) {
            requestedPaths.add(requested);
        }

        void sendOutcome(RemoteDevPackageClientOutcome outcome) {
            sendOutcomes.add(outcome);
        }

        @Override
        public RemoteDevPackageClientResult connect(Map<String, String> localHashes) {
            connects++;
            connectedHashes = Map.copyOf(localHashes);
            return RemoteDevPackageClientResult.connected(
                    requestedPaths.isEmpty() ? Set.of() : requestedPaths.remove());
        }

        @Override
        public RemoteDevPackageClientResult send(RemoteDevPackageDiff diff) throws IOException {
            if (failSends) {
                throw new IOException("boom");
            }
            if (blockSendsUntilClose) {
                sendStarted.countDown();
                try {
                    if (!releaseSend.await(5, TimeUnit.SECONDS)) {
                        throw new IOException("timed out waiting for close");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("interrupted", e);
                }
                if (closed) {
                    throw new IOException("closed");
                }
            }
            sentChanges.add(diff.changed().stream()
                    .map(RemoteDevPackageChange::relativePath)
                    .toList());
            RemoteDevPackageClientOutcome outcome = sendOutcomes.isEmpty()
                    ? RemoteDevPackageClientOutcome.SENT
                    : sendOutcomes.remove();
            return outcome == RemoteDevPackageClientOutcome.RECONNECT_REQUIRED
                    ? RemoteDevPackageClientResult.reconnectRequired()
                    : RemoteDevPackageClientResult.sent(diff.changed().size(), diff.deleted().size());
        }

        @Override
        public void startChangePolling(RemoteDevPackageReconnectListener listener) {
            pollingStarted = true;
            reconnectListener = listener;
        }

        @Override
        public void close() {
            closed = true;
            releaseSend.countDown();
        }
    }
}
