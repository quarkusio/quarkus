package io.quarkus.gradle.application.internal.dev;

import static io.quarkus.deployment.dev.BuildOutputChangeKind.MODIFIED;
import static io.quarkus.deployment.dev.BuildOutputChangeStatus.BUILD_SUCCEEDED;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.BASELINE_DROPPED;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.PENDING;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.RESTART_REQUIRED;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.SENT_APPLIED;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.deployment.dev.BuildOutputChanges;
import io.quarkus.deployment.dev.BuildOutputChangesApplyStatus;
import io.quarkus.deployment.dev.BuildOutputChangesPolicy;
import io.quarkus.deployment.dev.BuildOutputChangesServer;
import io.quarkus.deployment.dev.BuildOutputLiveReloadState;
import io.quarkus.deployment.dev.BuildOutputPathChange;
import io.quarkus.deployment.dev.DevModeContext;

class QuarkusApplicationDevSessionTest {

    @TempDir
    Path directory;

    @Test
    void enabledStateWritesOnlyMonotonicDeploymentScopedTriggerValues() throws Exception {
        Path trigger = directory.resolve("live-reload-replay.trigger");
        Files.writeString(trigger, "epoch=older\ngeneration=2\n", StandardCharsets.UTF_8);
        var session = new QuarkusApplicationDevSession(trigger, "11111111-1111-1111-1111-111111111111");

        session.liveReloadStateChanged(new BuildOutputLiveReloadState(0, true));
        session.liveReloadStateChanged(new BuildOutputLiveReloadState(1, false));
        assertThat(trigger).hasContent("epoch=older\ngeneration=2\n");

        session.liveReloadStateChanged(new BuildOutputLiveReloadState(2, true));
        assertThat(trigger).hasContent("""
                epoch=11111111-1111-1111-1111-111111111111
                generation=2
                """);

        session.liveReloadStateChanged(new BuildOutputLiveReloadState(1, true));
        assertThat(trigger).hasContent("""
                epoch=11111111-1111-1111-1111-111111111111
                generation=2
                """);
        session.close();
        session.liveReloadStateChanged(new BuildOutputLiveReloadState(3, true));
        assertThat(trigger).hasContent("""
                epoch=11111111-1111-1111-1111-111111111111
                generation=2
                """);
    }

    @Test
    void sameGenerationFromNewDeploymentChangesPersistentTrigger() {
        Path trigger = directory.resolve("live-reload-replay.trigger");
        var first = new QuarkusApplicationDevSession(trigger, "11111111-1111-1111-1111-111111111111");
        var second = new QuarkusApplicationDevSession(trigger, "22222222-2222-2222-2222-222222222222");

        first.liveReloadStateChanged(new BuildOutputLiveReloadState(2, true));
        second.liveReloadStateChanged(new BuildOutputLiveReloadState(2, true));

        assertThat(trigger).hasContent("""
                epoch=22222222-2222-2222-2222-222222222222
                generation=2
                """);
    }

    @Test
    void exposesDevUiUrlDiscoveredByTheRunningProcess() throws Exception {
        var session = new QuarkusApplicationDevSession();
        session.startIfNeededWithoutConnectionWait(ignored -> new QuarkusApplicationDevProcessHandle() {
            private final CompletableFuture<Integer> exitCode = new CompletableFuture<>();

            @Override
            public boolean isAlive() {
                return !exitCode.isDone();
            }

            @Override
            public CompletionStage<Integer> exitCode() {
                return exitCode;
            }

            @Override
            public Optional<String> devUiUrl() {
                return Optional.of("http://localhost:8080/q/dev-ui/continuous-testing");
            }

            @Override
            public void close() {
                exitCode.complete(0);
            }
        });

        assertThat(session.devUiUrl()).contains("http://localhost:8080/q/dev-ui/continuous-testing");
        session.close();
    }

    @Test
    void baselineBeforeReadyDoesNotCreateReloadBatch() {
        var session = new QuarkusApplicationDevSession();

        var result = session.accept(changes(1, "org/acme/App.class"));

        assertThat(result.outcome()).isEqualTo(BASELINE_DROPPED);
        assertThat(session.deliver(ignored -> BuildOutputChangesApplyStatus.APPLIED).outcome())
                .isEqualTo(BuildOutputChangesPolicy.Outcome.NOTHING_TO_SEND);
    }

    @Test
    void readySessionAcceptsAndDeliversReloadableChanges() {
        var session = new QuarkusApplicationDevSession();
        session.markReady();

        var result = session.accept(changes(1, "org/acme/App.class"));
        var delivery = session.deliver(ignored -> BuildOutputChangesApplyStatus.APPLIED);

        assertThat(result.outcome()).isEqualTo(PENDING);
        assertThat(delivery.outcome()).isEqualTo(SENT_APPLIED);
    }

    @Test
    void restartRequiredDoesNotErasePendingChanges() {
        var session = new QuarkusApplicationDevSession();
        session.markReady();
        session.accept(changes(1, "org/acme/App.class"));

        var restart = session.acceptRestartRequired(2);
        var delivery = session.deliver(ignored -> BuildOutputChangesApplyStatus.APPLIED);

        assertThat(restart.outcome()).isEqualTo(RESTART_REQUIRED);
        assertThat(delivery.changes().sequence()).isEqualTo(1);
    }

    @Test
    void slowDeliveryDoesNotBlockSessionClose() throws Exception {
        var session = new QuarkusApplicationDevSession();
        session.markReady();
        session.accept(changes(1, "org/acme/App.class"));
        var deliveryStarted = new CountDownLatch(1);
        var completeDelivery = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<BuildOutputChangesPolicy.Result> delivery = executor.submit(() -> session.deliver(changes -> {
                deliveryStarted.countDown();
                await(completeDelivery);
                return BuildOutputChangesApplyStatus.APPLIED;
            }));
            assertThat(deliveryStarted.await(5, SECONDS)).isTrue();

            Future<?> close = executor.submit(() -> {
                session.close();
                return null;
            });

            assertThat(close.get(1, SECONDS)).isNull();
            completeDelivery.countDown();
            assertThat(delivery.get(5, SECONDS).outcome()).isEqualTo(SENT_APPLIED);
        } finally {
            completeDelivery.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, SECONDS)).isTrue();
        }
    }

    @Test
    void closeUnblocksStartupConnectionWaitAndClosesLaunchedProcess() throws Exception {
        var server = new BlockingStartupServer();
        var session = new QuarkusApplicationDevSession(directory.resolve("startup-replay.trigger"),
                "11111111-1111-1111-1111-111111111111", ignored -> server);
        var processClosed = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> start = executor.submit(() -> session.startIfNeeded(ignored -> {
                return new QuarkusApplicationDevProcessHandle() {
                    private final CompletableFuture<Integer> exitCode = new CompletableFuture<>();

                    @Override
                    public boolean isAlive() {
                        return !exitCode.isDone();
                    }

                    @Override
                    public CompletionStage<Integer> exitCode() {
                        return exitCode;
                    }

                    @Override
                    public void close() {
                        exitCode.complete(0);
                        processClosed.countDown();
                    }
                };
            }));
            assertThat(server.sendStarted.await(5, SECONDS)).isTrue();

            session.close();

            assertThat(processClosed.await(5, SECONDS)).isTrue();
            assertThatThrownBy(() -> start.get(5, SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasRootCauseInstanceOf(IllegalStateException.class);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, SECONDS)).isTrue();
        }
    }

    @Test
    void changesWaitingForSlowDeliveryCoalesceIntoNextBatch() throws Exception {
        var session = new QuarkusApplicationDevSession();
        session.markReady();
        session.accept(changes(1, "org/acme/First.class"));
        var deliveryStarted = new CountDownLatch(1);
        var completeDelivery = new CountDownLatch(1);
        var acceptanceStarted = new CountDownLatch(1);
        var acceptanceCompleted = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<BuildOutputChangesPolicy.Result> delivery = executor.submit(() -> session.deliver(changes -> {
                deliveryStarted.countDown();
                await(completeDelivery);
                return BuildOutputChangesApplyStatus.APPLIED;
            }));
            assertThat(deliveryStarted.await(5, SECONDS)).isTrue();

            Future<BuildOutputChangesPolicy.Result> acceptance = executor.submit(() -> {
                acceptanceStarted.countDown();
                try {
                    return session.accept(changes(2, "org/acme/Second.class"));
                } finally {
                    acceptanceCompleted.countDown();
                }
            });
            assertThat(acceptanceStarted.await(5, SECONDS)).isTrue();
            assertThat(acceptanceCompleted.await(200, MILLISECONDS)).isFalse();

            completeDelivery.countDown();
            assertThat(delivery.get(5, SECONDS).outcome()).isEqualTo(SENT_APPLIED);
            assertThat(acceptance.get(5, SECONDS).outcome()).isEqualTo(PENDING);
            assertThat(session.accept(changes(3, "org/acme/Third.class")).outcome()).isEqualTo(PENDING);

            var delivered = new AtomicReference<BuildOutputChanges>();
            var nextDelivery = session.deliver(changes -> {
                delivered.set(changes);
                return BuildOutputChangesApplyStatus.APPLIED;
            });

            assertThat(nextDelivery.outcome()).isEqualTo(SENT_APPLIED);
            assertThat(delivered.get().sequence()).isEqualTo(3);
            assertThat(delivered.get().mainClassChanges())
                    .extracting(BuildOutputPathChange::changedPath)
                    .containsExactly(Path.of("build/classes/java/main/org/acme/Second.class"),
                            Path.of("build/classes/java/main/org/acme/Third.class"));
        } finally {
            completeDelivery.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, SECONDS)).isTrue();
        }
    }

    private static void await(CountDownLatch latch) throws IOException {
        try {
            if (!latch.await(5, SECONDS)) {
                throw new IOException("Timed out waiting to complete delivery");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting to complete delivery", e);
        }
    }

    private static final class BlockingStartupServer implements BuildOutputChangesServer {

        private final CountDownLatch sendStarted = new CountDownLatch(1);
        private final CountDownLatch closed = new CountDownLatch(1);
        private final CompletableFuture<Void> termination = new CompletableFuture<>();

        @Override
        public DevModeContext.ExternalBuildOutputTransport transport() {
            return DevModeContext.ExternalBuildOutputTransport.disabled();
        }

        @Override
        public BuildOutputChangesApplyStatus send(BuildOutputChanges changes) throws IOException {
            sendStarted.countDown();
            await(closed);
            return BuildOutputChangesApplyStatus.APPLIED;
        }

        @Override
        public CompletionStage<Void> termination() {
            return termination;
        }

        @Override
        public void close() {
            closed.countDown();
            termination.complete(null);
        }
    }

    private static BuildOutputChanges changes(long sequence, String relativePath) {
        Path outputRoot = Path.of("build/classes/java/main");
        return new BuildOutputChanges(sequence, BUILD_SUCCEEDED,
                List.of(new BuildOutputPathChange(outputRoot, outputRoot.resolve(relativePath), MODIFIED)),
                null, null, null, null, null, false, false);
    }
}
