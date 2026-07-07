package io.quarkus.gradle.application.internal.dev;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import io.quarkus.deployment.dev.BuildOutputChangeStatus;
import io.quarkus.deployment.dev.BuildOutputChanges;
import io.quarkus.deployment.dev.BuildOutputChangesPolicy;
import io.quarkus.deployment.dev.BuildOutputChangesServer;
import io.quarkus.deployment.dev.BuildOutputChangesTransports;
import io.quarkus.deployment.dev.BuildOutputLiveReloadState;

final class QuarkusApplicationDevSession implements AutoCloseable {

    private static final Logger log = Logging.getLogger(QuarkusApplicationDevSession.class);

    /*
     * The session monitor protects lifecycle, process/server references, readiness, and
     * sequence allocation. It also protects the startup-only baseline transition; after
     * readiness, policy state is serialized by policyLock. Code may enter policyLock
     * while holding the session monitor, but never reacquires the session monitor from
     * policy callbacks or while sending over the transport. Process, server, and trigger
     * close operations consequently run after lifecycle state has been published and the
     * session monitor has been released.
     */
    private final BuildOutputChangesPolicy policy = new BuildOutputChangesPolicy();
    private final Object policyLock = new Object();
    private final ContinuousBuildTriggerFile replayTrigger;
    private final BuildOutputChangesServerFactory serverFactory;
    private BuildOutputChangesServer buildOutputChangesServer;
    private QuarkusApplicationDevProcessHandle process;
    private long sequence;
    private boolean started;
    private boolean ready;
    private volatile boolean closed;

    QuarkusApplicationDevSession() {
        replayTrigger = null;
        serverFactory = BuildOutputChangesTransports::createTcpServer;
    }

    QuarkusApplicationDevSession(Path replayTriggerFile, String replayEpoch) {
        this(replayTriggerFile, replayEpoch, BuildOutputChangesTransports::createTcpServer);
    }

    QuarkusApplicationDevSession(Path replayTriggerFile, String replayEpoch,
            BuildOutputChangesServerFactory serverFactory) {
        replayTrigger = new ContinuousBuildTriggerFile(replayTriggerFile, replayEpoch);
        this.serverFactory = serverFactory;
    }

    boolean startIfNeeded(QuarkusApplicationDevProcessLauncher launcher) throws Exception {
        return startIfNeeded(launcher, true);
    }

    boolean startIfNeededWithoutConnectionWait(QuarkusApplicationDevProcessLauncher launcher) throws Exception {
        return startIfNeeded(launcher, false);
    }

    private boolean startIfNeeded(QuarkusApplicationDevProcessLauncher launcher, boolean waitForConnection) throws Exception {
        requireNonNull(launcher, "launcher");
        synchronized (this) {
            assertOpen();
            if (started) {
                return false;
            }
            started = true;
        }
        BuildOutputChangesServer server = null;
        QuarkusApplicationDevProcessHandle launchedProcess = null;
        try {
            server = serverFactory.create(this::liveReloadStateChanged);
            synchronized (this) {
                assertOpen();
                buildOutputChangesServer = server;
            }
            launchedProcess = requireNonNull(launcher.launch(server.transport()), "process");
            synchronized (this) {
                assertOpen();
                process = launchedProcess;
            }
            if (waitForConnection) {
                server.send(connectionProbe());
            }
            synchronized (this) {
                assertOpen();
            }
        } catch (Exception e) {
            synchronized (this) {
                if (server != null && buildOutputChangesServer == server) {
                    buildOutputChangesServer = null;
                }
                if (process == launchedProcess) {
                    process = null;
                }
                started = false;
            }
            if (launchedProcess != null) {
                try {
                    launchedProcess.close();
                } catch (Exception closeFailure) {
                    e.addSuppressed(closeFailure);
                }
            }
            if (server != null) {
                try {
                    server.close();
                } catch (IOException closeFailure) {
                    e.addSuppressed(closeFailure);
                }
            }
            throw e;
        }
        return true;
    }

    synchronized BuildOutputChangesPolicy.Result acceptStartupBaseline(BuildOutputChanges changes) {
        assertOpen();
        synchronized (policyLock) {
            return policy.acceptStartupBaseline(changes);
        }
    }

    synchronized BuildOutputChangesPolicy.Result acceptRestartRequired(long sequence) {
        assertOpen();
        synchronized (policyLock) {
            return policy.acceptRestartRequired(sequence);
        }
    }

    synchronized long nextSequence() {
        assertOpen();
        return ++sequence;
    }

    synchronized void markReady() {
        assertOpen();
        ready = true;
    }

    synchronized boolean isReady() {
        return ready;
    }

    synchronized Optional<String> devUiUrl() {
        return process == null ? Optional.empty() : process.devUiUrl();
    }

    synchronized boolean isHealthy() {
        return ready && process != null && process.isAlive()
                && buildOutputChangesServer != null
                && !buildOutputChangesServer.termination().toCompletableFuture().isDone();
    }

    synchronized CompletionStage<Integer> processExit() {
        return process == null ? CompletableFuture.failedFuture(new IllegalStateException("Dev process has not started"))
                : process.exitCode();
    }

    synchronized CompletionStage<Void> transportTermination() {
        return buildOutputChangesServer == null
                ? CompletableFuture.failedFuture(new IllegalStateException("Dev transport has not started"))
                : buildOutputChangesServer.termination();
    }

    synchronized BuildOutputChangesPolicy.Result accept(BuildOutputChanges changes) {
        assertOpen();
        if (!ready) {
            // No sender can run before markReady(), so the startup baseline is the
            // only policy transition that may rely on the session monitor alone.
            return policy.acceptStartupBaseline(changes);
        }
        synchronized (policyLock) {
            return policy.accept(changes);
        }
    }

    BuildOutputChangesPolicy.Result deliver(BuildOutputChangesPolicy.Sender sender) {
        BuildOutputChangesPolicy.Sender readySender;
        synchronized (this) {
            assertOpen();
            readySender = ready ? sender : ignored -> {
                throw new IllegalStateException("Dev session must not deliver reload batches before it is ready");
            };
        }
        // Do not retain the session monitor while the sender waits for a transport
        // response. The receiver may asynchronously publish live-reload state.
        synchronized (policyLock) {
            return policy.deliver(readySender);
        }
    }

    BuildOutputChangesPolicy.Result deliver() {
        BuildOutputChangesServer server;
        synchronized (this) {
            assertOpen();
            if (!ready) {
                throw new IllegalStateException("Dev session must not deliver reload batches before it is ready");
            }
            server = buildOutputChangesServer;
        }
        // The server call may block until the child replies. Only policy ordering,
        // not lifecycle inspection or close(), is serialized for that interval.
        synchronized (policyLock) {
            if (server == null) {
                return policy.discardPending("Dev session has no build-output changes server");
            }
            return policy.deliver(server::send);
        }
    }

    synchronized boolean isClosed() {
        return closed;
    }

    boolean requestRecovery() throws IOException {
        return replayTrigger != null && replayTrigger.publishNext();
    }

    @Override
    public void close() throws Exception {
        BuildOutputChangesServer server;
        QuarkusApplicationDevProcessHandle runningProcess;
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            server = buildOutputChangesServer;
            runningProcess = process;
        }
        if (replayTrigger != null) {
            replayTrigger.close();
        }
        Exception failure = null;
        if (server != null) {
            try {
                server.close();
            } catch (IOException e) {
                failure = e;
            }
        }
        if (runningProcess != null) {
            try {
                runningProcess.close();
            } catch (Exception e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    void liveReloadStateChanged(BuildOutputLiveReloadState state) {
        if (!state.enabled() || state.generation() == 0 || replayTrigger == null) {
            return;
        }
        try {
            if (replayTrigger.publish(state.generation())) {
                log.lifecycle("Live reload enabled; requesting a safe Gradle replay iteration.");
            }
        } catch (IOException e) {
            log.error("Failed to update Quarkus dev replay trigger " + replayTrigger.path(), e);
        }
    }

    private static BuildOutputChanges connectionProbe() {
        // Sending goes through the authenticated transport and waits for the
        // Quarkus dev process to respond. A cancelled sequence 0 advances the
        // transport sequence without creating a visible build or test
        // compilation failure, so this proves connectivity without becoming a
        // reloadable build-output update.
        return new BuildOutputChanges(0, BuildOutputChangeStatus.BUILD_CANCELLED, List.of(), List.of(), null, null, null,
                null,
                false, false);
    }

    private void assertOpen() {
        if (closed) {
            throw new IllegalStateException("Dev session is closed");
        }
    }
}
