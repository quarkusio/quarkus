package io.quarkus.gradle.application.internal.remotedev;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.deployment.dev.remotedev.RemoteDevPackageClient;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageClientOutcome;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageClientResult;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageDiff;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageSnapshot;
import io.quarkus.gradle.application.internal.dev.ContinuousBuildTriggerFile;

public final class QuarkusApplicationRemoteDevSession implements AutoCloseable {

    private static final int MAX_RECONCILIATION_CYCLES = 4;

    /*
     * deliveryLock serializes complete delivery/reconciliation cycles and is
     * always acquired before stateLock. stateLock owns the client, delivered
     * baseline, sequence, reconnect/polling state, and close state. Network I/O,
     * client close, and reconnect-trigger publication stay outside stateLock;
     * commit deliberately holds it while persisting and publishing the baseline.
     */
    private final Object deliveryLock = new Object();
    private final Object stateLock = new Object();
    private final ContinuousBuildTriggerFile reconnectTrigger;
    private RemoteDevPackageClient client;
    private RemoteDevPackageSnapshot delivered = RemoteDevPackageSnapshot.empty();
    private long sequence;
    private long reconnectVersion;
    private boolean reconnectRequired = true;
    private boolean reconnectTriggerPublished = true;
    private boolean pollingStarted;
    private boolean closed;

    QuarkusApplicationRemoteDevSession() {
        reconnectTrigger = null;
    }

    QuarkusApplicationRemoteDevSession(Path reconnectTriggerFile, String reconnectEpoch) {
        reconnectTrigger = new ContinuousBuildTriggerFile(reconnectTriggerFile, reconnectEpoch);
    }

    public long nextSequence() throws IOException {
        synchronized (stateLock) {
            assertOpen();
            sequence = Math.incrementExact(sequence);
            return sequence;
        }
    }

    public DeliveryResult deliver(RemoteDevPackageSnapshot current, Path packageRoot,
            Path snapshotFile, ClientConnector connector) throws IOException {
        synchronized (deliveryLock) {
            assertOpen();
            RemoteDevPackageClient currentClient = client(connector);
            long requiredVersion = reconnectVersion();
            List<RemoteDevPackageDiff> requestedBatches = new ArrayList<>();
            if (requiredVersion != 0) {
                reconcile(currentClient, current, packageRoot, requestedBatches);
                clearReconnectRequired(requiredVersion);
                try {
                    startPollingIfNeeded(currentClient);
                } catch (IOException | RuntimeException e) {
                    markReconnectRequired();
                    throw e;
                }
                commit(current, snapshotFile);
                int requested = requestedBatches.stream().mapToInt(batch -> batch.changed().size()).sum();
                return new DeliveryResult("CONNECTED", requested, 0, List.copyOf(requestedBatches));
            }

            RemoteDevPackageDiff diff = current.diffSince(delivered, packageRoot);
            if (diff.isEmpty()) {
                return new DeliveryResult("NO_CHANGES", 0, 0, List.of());
            }
            RemoteDevPackageClientResult result = currentClient.send(diff);
            if (result.outcome() == RemoteDevPackageClientOutcome.RECONNECT_REQUIRED) {
                long version = requireReconnectTrigger();
                reconcile(currentClient, current, packageRoot, requestedBatches);
                clearReconnectRequired(version);
            } else if (result.outcome() != RemoteDevPackageClientOutcome.SENT) {
                throw new IOException("Unexpected remote-dev package send outcome " + result.outcome());
            }
            commit(current, snapshotFile);
            return new DeliveryResult("SENT", diff.changed().size(), diff.deleted().size(),
                    List.copyOf(requestedBatches));
        }
    }

    @Override
    public void close() throws IOException {
        RemoteDevPackageClient clientToClose;
        synchronized (stateLock) {
            if (closed) {
                return;
            }
            closed = true;
            clientToClose = client;
            client = null;
        }
        if (reconnectTrigger != null) {
            reconnectTrigger.close();
        }
        if (clientToClose != null) {
            clientToClose.close();
        }
    }

    private RemoteDevPackageClient client(ClientConnector connector) throws IOException {
        synchronized (stateLock) {
            assertOpen();
            if (client != null) {
                return client;
            }
        }
        RemoteDevPackageClient created = requireNonNull(connector.connect(), "client");
        synchronized (stateLock) {
            if (closed) {
                created.close();
                throw new IOException("Remote dev session is closed");
            }
            client = created;
            reconnectRequired = true;
            reconnectVersion++;
            reconnectTriggerPublished = true;
            return created;
        }
    }

    private void reconcile(RemoteDevPackageClient currentClient, RemoteDevPackageSnapshot current, Path packageRoot,
            List<RemoteDevPackageDiff> requestedBatches) throws IOException {
        for (int cycle = 1; cycle <= MAX_RECONCILIATION_CYCLES; cycle++) {
            RemoteDevPackageClientResult connected = currentClient.connect(current.hashes());
            if (connected.outcome() != RemoteDevPackageClientOutcome.CONNECTED) {
                throw new IOException("Unexpected remote-dev package connect outcome " + connected.outcome());
            }
            RemoteDevPackageDiff requested = current.requestedFiles(connected.requestedPaths(), packageRoot);
            if (requested.isEmpty()) {
                return;
            }
            requestedBatches.add(requested);
            RemoteDevPackageClientResult sent = currentClient.send(requested);
            if (sent.outcome() == RemoteDevPackageClientOutcome.SENT) {
                return;
            }
            if (sent.outcome() != RemoteDevPackageClientOutcome.RECONNECT_REQUIRED) {
                throw new IOException("Unexpected remote-dev reconciliation outcome " + sent.outcome());
            }
        }
        throw new IOException("Remote dev package reconciliation did not stabilize after "
                + MAX_RECONCILIATION_CYCLES + " connection attempts");
    }

    private void startPollingIfNeeded(RemoteDevPackageClient currentClient) throws IOException {
        synchronized (stateLock) {
            assertOpen();
            if (pollingStarted) {
                return;
            }
        }
        currentClient.startChangePolling(this::asynchronousReconnectRequired);
        synchronized (stateLock) {
            assertOpen();
            pollingStarted = true;
        }
    }

    private void asynchronousReconnectRequired() throws IOException {
        requireReconnectTrigger();
    }

    private long requireReconnectTrigger() throws IOException {
        long version;
        boolean publish;
        synchronized (stateLock) {
            if (closed) {
                return reconnectVersion;
            }
            if (!reconnectRequired) {
                reconnectRequired = true;
                reconnectVersion++;
                reconnectTriggerPublished = false;
            }
            version = reconnectVersion;
            publish = !reconnectTriggerPublished && reconnectTrigger != null;
        }
        if (publish) {
            reconnectTrigger.publish(version - 1);
            synchronized (stateLock) {
                if (!closed && reconnectVersion == version) {
                    reconnectTriggerPublished = true;
                }
            }
        }
        return version;
    }

    private long reconnectVersion() {
        synchronized (stateLock) {
            return reconnectRequired ? reconnectVersion : 0;
        }
    }

    private void markReconnectRequired() {
        synchronized (stateLock) {
            if (!closed && !reconnectRequired) {
                reconnectRequired = true;
                reconnectVersion++;
                reconnectTriggerPublished = false;
            }
        }
    }

    private void clearReconnectRequired(long version) {
        synchronized (stateLock) {
            if (reconnectVersion == version) {
                reconnectRequired = false;
                reconnectTriggerPublished = false;
            }
        }
    }

    private void commit(RemoteDevPackageSnapshot current, Path snapshotFile) throws IOException {
        synchronized (stateLock) {
            assertOpen();
            current.write(snapshotFile);
            delivered = current;
        }
    }

    private void assertOpen() throws IOException {
        synchronized (stateLock) {
            if (closed) {
                throw new IOException("Remote dev session is closed");
            }
        }
    }

    public interface ClientConnector {
        RemoteDevPackageClient connect() throws IOException;
    }

    public record DeliveryResult(String outcome, int changed, int deleted,
            List<RemoteDevPackageDiff> requestedBatches) {
        public DeliveryResult {
            requestedBatches = List.copyOf(requestedBatches);
        }
    }
}
