package io.quarkus.gradle.application.internal.dev;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.LongSupplier;

import javax.inject.Inject;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.deployment.internal.Deployment;
import org.gradle.deployment.internal.DeploymentHandle;

import io.quarkus.deployment.dev.BuildOutputChangeStatus;
import io.quarkus.deployment.dev.BuildOutputChanges;
import io.quarkus.deployment.dev.BuildOutputChangesPolicy;
import io.quarkus.deployment.dev.BuildOutputChangesTransports;
import io.quarkus.deployment.dev.BuildOutputFailureKind;

/**
 * Gradle has no public build-session-scoped service for continuous builds.
 * This handle intentionally uses Gradle's internal deployment API and keeps the
 * session ownership boundary narrow so it can be replaced when Gradle exposes a
 * public alternative.
 * <p>
 * The registered handle is a long-lived supervisor. It owns at most one
 * child/transport generation and replaces that generation after an unexpected
 * post-start failure without replacing Gradle's registry entry.
 */
public class QuarkusApplicationDevDeploymentHandle implements DeploymentHandle {

    private static final Logger log = Logging.getLogger(QuarkusApplicationDevDeploymentHandle.class);
    private static final Duration CLEANUP_TIMEOUT = Duration.ofSeconds(20);
    private static final long FAILURE_WINDOW_NANOS = Duration.ofSeconds(60).toNanos();
    private static final int MAX_AUTOMATIC_RECOVERIES = 3;

    private final String configFingerprint;
    private final Path closeReceiptFile;
    private final Path replayTriggerFile;
    private final QuarkusApplicationDevProcessLauncher processLauncher;
    private final BuildOutputChangesServerFactory serverFactory;
    private final LongSupplier nanoTime;
    private final String initialReplayEpoch;
    /*
     * This monitor owns the generation state machine, recovery leadership,
     * failure window, and cleanup-executor reference. Potentially blocking child,
     * transport, cleanup, and future waits run after the relevant generation and
     * shared future have been captured. Completion callbacks may re-enter the
     * monitor only to publish a transition for the still-current generation.
     */
    private final Deque<Long> recentFailures = new ArrayDeque<>();
    private long nextGenerationId;
    private Generation current;
    private CompletableFuture<QuarkusApplicationDevDeployments.Acquisition> restartInProgress;
    private ExecutorService cleanupExecutor;
    private boolean closed;
    private String terminalFailure;

    @Inject
    public QuarkusApplicationDevDeploymentHandle(String configFingerprint,
            GradleNativeDevModeLauncher.Parameters launchParameters,
            Path closeReceiptFile,
            Path replayTriggerFile,
            String replayEpoch) {
        this(configFingerprint, closeReceiptFile, replayTriggerFile,
                transport -> GradleNativeDevModeLauncher.launch(launchParameters, transport), System::nanoTime,
                replayEpoch, BuildOutputChangesTransports::createTcpServer);
    }

    QuarkusApplicationDevDeploymentHandle(String configFingerprint, Path closeReceiptFile, Path replayTriggerFile,
            QuarkusApplicationDevProcessLauncher processLauncher, LongSupplier nanoTime) {
        this(configFingerprint, closeReceiptFile, replayTriggerFile, processLauncher, nanoTime,
                UUID.randomUUID().toString(), BuildOutputChangesTransports::createTcpServer);
    }

    QuarkusApplicationDevDeploymentHandle(String configFingerprint, Path closeReceiptFile, Path replayTriggerFile,
            QuarkusApplicationDevProcessLauncher processLauncher, BuildOutputChangesServerFactory serverFactory,
            LongSupplier nanoTime) {
        this(configFingerprint, closeReceiptFile, replayTriggerFile, processLauncher, nanoTime,
                UUID.randomUUID().toString(), serverFactory);
    }

    private QuarkusApplicationDevDeploymentHandle(String configFingerprint, Path closeReceiptFile, Path replayTriggerFile,
            QuarkusApplicationDevProcessLauncher processLauncher, LongSupplier nanoTime, String initialReplayEpoch,
            BuildOutputChangesServerFactory serverFactory) {
        this.configFingerprint = configFingerprint;
        this.closeReceiptFile = closeReceiptFile;
        this.replayTriggerFile = replayTriggerFile;
        this.processLauncher = processLauncher;
        this.serverFactory = serverFactory;
        this.nanoTime = nanoTime;
        this.initialReplayEpoch = initialReplayEpoch;
    }

    public String configFingerprint() {
        return configFingerprint;
    }

    public long nextSequence() {
        return currentSession().nextSequence();
    }

    public boolean ready() {
        detectHealthLoss();
        synchronized (this) {
            return !closed && terminalFailure == null && current != null && current.state == GenerationState.READY
                    && current.session.isHealthy();
        }
    }

    public Optional<String> devUiUrl() {
        return currentSession().devUiUrl();
    }

    public String acceptStartupBaselineOutcome(BuildOutputChanges changes) {
        return currentSession().acceptStartupBaseline(changes).outcome().name();
    }

    public String acceptReadyChangesOutcome(BuildOutputChanges changes) {
        return currentSession().accept(changes).outcome().name();
    }

    public String deliverReadyChangesOutcome() {
        Generation generation = currentGeneration();
        BuildOutputChangesPolicy.Result result = generation.session.deliver();
        if (result.outcome() == BuildOutputChangesPolicy.Outcome.SEND_FAILED) {
            failGeneration(generation, FailureSource.DELIVERY, result.failure());
        }
        return result.outcome().name();
    }

    public String acceptRestartRequiredOutcome(long sequence) {
        return currentSession().acceptRestartRequired(sequence).outcome().name();
    }

    @Override
    public boolean isRunning() {
        detectHealthLoss();
        synchronized (this) {
            return !closed && terminalFailure == null && current != null && current.state == GenerationState.READY
                    && current.session.isHealthy();
        }
    }

    @Override
    public void start(Deployment deployment) {
        try {
            startGeneration(false);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to launch Quarkus dev mode", e);
        }
    }

    QuarkusApplicationDevDeployments.Acquisition acquire() {
        detectHealthLoss();
        CompletableFuture<QuarkusApplicationDevDeployments.Acquisition> sharedRestart;
        CompletableFuture<Void> cleanup;
        boolean leader;
        synchronized (this) {
            assertAvailable();
            if (current != null && current.state == GenerationState.READY && current.session.isHealthy()) {
                return QuarkusApplicationDevDeployments.Acquisition.EXISTING_READY;
            }
            // Exactly one caller owns cleanup and restart. Followers share the same
            // result so concurrent Gradle task actions cannot create two children.
            if (restartInProgress != null) {
                leader = false;
            } else {
                if (current == null || current.state == GenerationState.STARTING) {
                    throw new IllegalStateException("Quarkus dev mode generation is not available for recovery");
                }
                restartInProgress = new CompletableFuture<>();
                leader = true;
            }
            sharedRestart = restartInProgress;
            cleanup = current.cleanup;
        }
        if (!leader) {
            return awaitRestart(sharedRestart);
        }
        try {
            awaitCleanup(cleanup);
            startGeneration(true);
            sharedRestart.complete(QuarkusApplicationDevDeployments.Acquisition.RESTARTED_AFTER_FAILURE);
            return QuarkusApplicationDevDeployments.Acquisition.RESTARTED_AFTER_FAILURE;
        } catch (Exception e) {
            sharedRestart.completeExceptionally(e);
            throw new IllegalStateException("Failed to recover Quarkus dev mode", e);
        } finally {
            synchronized (this) {
                if (restartInProgress == sharedRestart) {
                    restartInProgress = null;
                }
            }
        }
    }

    @Override
    public void stop() {
        Generation generation;
        boolean closeGeneration;
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            generation = current;
            closeGeneration = generation != null
                    && generation.state != GenerationState.STOPPING
                    && generation.state != GenerationState.STOPPED;
            if (closeGeneration) {
                generation.state = GenerationState.STOPPING;
            }
        }
        Exception failure = null;
        if (generation != null) {
            if (closeGeneration) {
                failure = closeGeneration(generation);
            } else {
                try {
                    awaitCleanup(generation.cleanup);
                } catch (Exception e) {
                    failure = e;
                }
            }
        }
        failure = stopCleanupExecutor(failure);
        try {
            writeCloseReceipt();
        } catch (IOException e) {
            failure = addFailure(failure, e);
        }
        if (failure != null) {
            throw new IllegalStateException("Failed to stop Quarkus dev mode", failure);
        }
    }

    public boolean reportBuildFailure(BuildOutputFailureKind failureKind, String taskPath) {
        Generation generation;
        synchronized (this) {
            if (closed || terminalFailure != null || current == null || current.state != GenerationState.READY) {
                return false;
            }
            generation = current;
        }
        long sequence = generation.session.nextSequence();
        var failure = new BuildOutputChanges(sequence, BuildOutputChangeStatus.BUILD_FAILED, failureKind,
                List.of(), List.of(), List.of(), List.of(),
                "Gradle task '" + taskPath + "' failed; see the Gradle output for details.", null, false, false);
        generation.session.accept(failure);
        BuildOutputChangesPolicy.Result delivered = generation.session.deliver();
        if (delivered.outcome() == BuildOutputChangesPolicy.Outcome.SEND_FAILED) {
            failGeneration(generation, FailureSource.DELIVERY, delivered.failure());
        }
        return true;
    }

    private void startGeneration(boolean recovery) throws Exception {
        Generation generation;
        synchronized (this) {
            assertAvailable();
            if (current != null && current.state != GenerationState.STOPPED) {
                throw new IllegalStateException("A Quarkus dev mode generation is already active");
            }
            long generationId = ++nextGenerationId;
            String replayEpoch = generationId == 1 ? initialReplayEpoch : UUID.randomUUID().toString();
            generation = new Generation(generationId,
                    new QuarkusApplicationDevSession(replayTriggerFile, replayEpoch, serverFactory));
            current = generation;
        }
        try {
            generation.session.startIfNeeded(processLauncher);
            generation.session.markReady();
            if (!generation.session.isHealthy()) {
                throw new IllegalStateException("Quarkus dev mode child or transport stopped during startup");
            }
            synchronized (this) {
                assertAvailable();
                if (current != generation || generation.state != GenerationState.STARTING) {
                    throw new IllegalStateException("Quarkus dev mode generation was cancelled during startup");
                }
                generation.state = GenerationState.READY;
                generation.readyAtNanos = nanoTime.getAsLong();
            }
            registerFailureCallbacks(generation);
            if (recovery) {
                log.lifecycle("Recovered Quarkus dev mode with generation {}.", generation.id);
            }
        } catch (Exception e) {
            synchronized (this) {
                if (current == generation && generation.state == GenerationState.STARTING) {
                    generation.state = GenerationState.STOPPING;
                }
            }
            Exception closeFailure = closeGeneration(generation);
            if (closeFailure != null) {
                e.addSuppressed(closeFailure);
            }
            throw e;
        }
    }

    private void registerFailureCallbacks(Generation generation) {
        // Both asynchronous sources race through failGeneration(), whose current
        // generation/state check linearizes the transition and schedules cleanup once.
        generation.session.processExit().whenComplete((exitCode, failure) -> {
            Throwable cause = failure == null ? null : unwrap(failure);
            generation.exitCode = exitCode;
            failGeneration(generation, FailureSource.CHILD, cause);
        });
        generation.session.transportTermination().whenComplete((ignored, failure) -> {
            failGeneration(generation, FailureSource.TRANSPORT, failure == null ? null : unwrap(failure));
        });
    }

    private void detectHealthLoss() {
        Generation generation;
        synchronized (this) {
            generation = current;
            if (closed || terminalFailure != null || generation == null || generation.state != GenerationState.READY) {
                return;
            }
        }
        if (!generation.session.isHealthy()) {
            failGeneration(generation, FailureSource.HEALTH_CHECK, null);
        }
    }

    private void failGeneration(Generation generation, FailureSource source, Throwable cause) {
        boolean publishRecovery;
        String summary;
        synchronized (this) {
            if (closed || current != generation || generation.state != GenerationState.READY) {
                return;
            }
            generation.state = GenerationState.FAILED;
            summary = failureSummary(source, generation.exitCode);
            publishRecovery = permitRecovery(generation);
            if (!publishRecovery) {
                terminalFailure = "Quarkus dev mode failed repeatedly; cancel and restart the continuous task.";
            }
        }
        log.error(summary);
        if (cause != null) {
            log.debug(summary, cause);
        }
        if (publishRecovery) {
            try {
                if (generation.session.requestRecovery()) {
                    log.lifecycle("Requesting a Gradle iteration to recover Quarkus dev mode.");
                }
            } catch (IOException e) {
                synchronized (this) {
                    terminalFailure = "Failed to request a Gradle iteration for Quarkus dev mode recovery.";
                }
                log.error("Failed to request a Gradle iteration for Quarkus dev mode recovery.");
                log.debug("Failed to request a Gradle iteration for Quarkus dev mode recovery.", e);
            }
        } else {
            log.error(terminalFailure);
        }
        scheduleCleanup(generation);
    }

    private boolean permitRecovery(Generation generation) {
        long now = nanoTime.getAsLong();
        if (now - generation.readyAtNanos >= FAILURE_WINDOW_NANOS) {
            recentFailures.clear();
        }
        while (!recentFailures.isEmpty() && now - recentFailures.peekFirst() >= FAILURE_WINDOW_NANOS) {
            recentFailures.removeFirst();
        }
        recentFailures.addLast(now);
        return recentFailures.size() <= MAX_AUTOMATIC_RECOVERIES;
    }

    private void scheduleCleanup(Generation generation) {
        ExecutorService executor;
        synchronized (this) {
            if (generation.state != GenerationState.FAILED) {
                return;
            }
            generation.state = GenerationState.STOPPING;
            executor = cleanupExecutor();
        }
        try {
            executor.execute(() -> {
                Exception failure = closeGeneration(generation);
                if (failure != null) {
                    synchronized (this) {
                        terminalFailure = "Failed to clean up a stopped Quarkus dev mode generation.";
                    }
                    log.error(terminalFailure);
                    log.debug(terminalFailure, failure);
                }
            });
        } catch (RuntimeException e) {
            synchronized (this) {
                terminalFailure = "Failed to schedule Quarkus dev mode generation cleanup.";
                generation.state = GenerationState.STOPPED;
            }
            generation.cleanup.completeExceptionally(e);
            log.error(terminalFailure);
            log.debug(terminalFailure, e);
        }
    }

    private Exception closeGeneration(Generation generation) {
        // Closing a child or transport can block and complete asynchronous callbacks;
        // it must happen without this handle's monitor.
        Exception failure = null;
        try {
            generation.session.close();
        } catch (Exception e) {
            failure = e;
        }
        synchronized (this) {
            generation.state = GenerationState.STOPPED;
        }
        if (failure == null) {
            generation.cleanup.complete(null);
        } else {
            generation.cleanup.completeExceptionally(failure);
        }
        return failure;
    }

    private synchronized ExecutorService cleanupExecutor() {
        if (cleanupExecutor == null) {
            cleanupExecutor = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "Quarkus Gradle dev generation cleanup");
                thread.setDaemon(true);
                return thread;
            });
        }
        return cleanupExecutor;
    }

    private Exception stopCleanupExecutor(Exception failure) {
        ExecutorService executor;
        synchronized (this) {
            executor = cleanupExecutor;
            cleanupExecutor = null;
        }
        if (executor == null) {
            return failure;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(CLEANUP_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
                failure = addFailure(failure,
                        new IOException("Timed out stopping Quarkus dev mode generation cleanup worker"));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
            failure = addFailure(failure,
                    new IOException("Interrupted stopping Quarkus dev mode generation cleanup worker", e));
        }
        return failure;
    }

    private static void awaitCleanup(CompletableFuture<Void> cleanup) throws Exception {
        try {
            cleanup.get(CLEANUP_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted waiting for Quarkus dev mode generation cleanup", e);
        } catch (ExecutionException e) {
            throw new IOException("Quarkus dev mode generation cleanup failed", unwrap(e));
        } catch (TimeoutException e) {
            throw new IOException("Timed out waiting for Quarkus dev mode generation cleanup", e);
        }
    }

    private static QuarkusApplicationDevDeployments.Acquisition awaitRestart(
            CompletableFuture<QuarkusApplicationDevDeployments.Acquisition> restart) {
        try {
            return restart.get(CLEANUP_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for Quarkus dev mode recovery", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Quarkus dev mode recovery failed", unwrap(e));
        } catch (TimeoutException e) {
            throw new IllegalStateException("Timed out waiting for Quarkus dev mode recovery", e);
        }
    }

    private synchronized Generation currentGeneration() {
        assertAvailable();
        if (current == null || current.state != GenerationState.READY || !current.session.isHealthy()) {
            throw new IllegalStateException("Quarkus dev mode generation is not ready");
        }
        return current;
    }

    private QuarkusApplicationDevSession currentSession() {
        return currentGeneration().session;
    }

    private synchronized void assertAvailable() {
        if (closed) {
            throw new IllegalStateException("Quarkus dev mode deployment is closed");
        }
        if (terminalFailure != null) {
            throw new IllegalStateException(terminalFailure);
        }
    }

    private static String failureSummary(FailureSource source, Integer exitCode) {
        return switch (source) {
            case CHILD -> exitCode == null
                    ? "Quarkus dev mode child process terminated unexpectedly."
                    : "Quarkus dev mode child process exited unexpectedly with code " + exitCode + ".";
            case TRANSPORT -> "Quarkus external build-output transport terminated unexpectedly.";
            case DELIVERY -> "Quarkus external build-output delivery failed.";
            case HEALTH_CHECK -> "Quarkus dev mode child or transport is no longer running.";
        };
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while ((current instanceof ExecutionException || current instanceof java.util.concurrent.CompletionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static Exception addFailure(Exception failure, Exception additional) {
        if (failure == null) {
            return additional;
        }
        failure.addSuppressed(additional);
        return failure;
    }

    private void writeCloseReceipt() throws IOException {
        Files.createDirectories(closeReceiptFile.getParent());
        Files.writeString(closeReceiptFile, "closed\n", StandardCharsets.UTF_8);
    }

    private enum GenerationState {
        STARTING,
        READY,
        FAILED,
        STOPPING,
        STOPPED
    }

    private enum FailureSource {
        CHILD,
        TRANSPORT,
        DELIVERY,
        HEALTH_CHECK
    }

    private static final class Generation {

        private final long id;
        private final QuarkusApplicationDevSession session;
        private final CompletableFuture<Void> cleanup = new CompletableFuture<>();
        private GenerationState state = GenerationState.STARTING;
        private long readyAtNanos;
        private Integer exitCode;

        private Generation(long id, QuarkusApplicationDevSession session) {
            this.id = id;
            this.session = session;
        }
    }
}
