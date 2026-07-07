package io.quarkus.gradle.application.internal.dev;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.deployment.dev.BuildOutputChangeStatus;
import io.quarkus.deployment.dev.BuildOutputChanges;
import io.quarkus.deployment.dev.BuildOutputChangesApplyStatus;
import io.quarkus.deployment.dev.BuildOutputChangesDeliveryKind;
import io.quarkus.deployment.dev.BuildOutputChangesServer;
import io.quarkus.deployment.dev.BuildOutputFailureKind;
import io.quarkus.deployment.dev.BuildOutputLiveReloadState;
import io.quarkus.deployment.dev.DevModeContext;

class QuarkusApplicationDevDeploymentHandleTest {

    @TempDir
    Path directory;

    @Test
    void childExitTriggersOneCleanupAndFreshGeneration() throws Exception {
        Fixture fixture = new Fixture();
        QuarkusApplicationDevDeploymentHandle handle = fixture.start();

        fixture.processes.get(0).exit(23);
        String failedGenerationEpoch = fixture.triggerEpoch();

        assertThat(handle.isRunning()).isFalse();
        assertThat(fixture.triggerGeneration()).isEqualTo(1);
        assertThat(fixture.processes.get(0).closed.get(5, SECONDS)).isNull();
        assertThat(handle.acquire()).isEqualTo(QuarkusApplicationDevDeployments.Acquisition.RESTARTED_AFTER_FAILURE);
        assertThat(handle.isRunning()).isTrue();
        assertThat(fixture.processes).hasSize(2);
        assertThat(fixture.servers).hasSize(2);
        fixture.servers.get(1).liveReloadStateChanged(2, true);
        assertThat(fixture.triggerEpoch()).isNotEqualTo(failedGenerationEpoch);
        assertThat(fixture.triggerGeneration()).isEqualTo(2);
        assertThat(fixture.closeReceipt).doesNotExist();

        handle.stop();

        assertThat(fixture.closeReceipt).hasContent("closed\n");
        assertThat(fixture.processes.get(1).closed.get(5, SECONDS)).isNull();
    }

    @Test
    void transportFailureAndChildExitAreLinearizedIntoOneRecovery() throws Exception {
        Fixture fixture = new Fixture();
        QuarkusApplicationDevDeploymentHandle handle = fixture.start();

        fixture.servers.get(0).fail();
        fixture.processes.get(0).exit(1);

        assertThat(fixture.triggerGeneration()).isEqualTo(1);
        assertThat(handle.acquire()).isEqualTo(QuarkusApplicationDevDeployments.Acquisition.RESTARTED_AFTER_FAILURE);
        assertThat(fixture.processes).hasSize(2);
        assertThat(fixture.servers).hasSize(2);
        handle.stop();
    }

    @Test
    void concurrentAcquisitionsWaitForOneCleanupAndCreateOneReplacement() throws Exception {
        Fixture fixture = new Fixture();
        QuarkusApplicationDevDeploymentHandle handle = fixture.start();
        FakeProcess failedProcess = fixture.processes.get(0);
        failedProcess.blockClose();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            failedProcess.exit(1);
            assertThat(failedProcess.closeStarted.await(5, SECONDS)).isTrue();

            Future<QuarkusApplicationDevDeployments.Acquisition> first = executor.submit(handle::acquire);
            Future<QuarkusApplicationDevDeployments.Acquisition> second = executor.submit(handle::acquire);
            assertThat(fixture.processes).hasSize(1);

            failedProcess.allowClose();

            assertThat(List.of(first.get(5, SECONDS), second.get(5, SECONDS)))
                    .contains(QuarkusApplicationDevDeployments.Acquisition.RESTARTED_AFTER_FAILURE)
                    .allSatisfy(acquisition -> assertThat(acquisition).isIn(
                            QuarkusApplicationDevDeployments.Acquisition.RESTARTED_AFTER_FAILURE,
                            QuarkusApplicationDevDeployments.Acquisition.EXISTING_READY));
            assertThat(fixture.processes).hasSize(2);
            assertThat(fixture.servers).hasSize(2);
        } finally {
            failedProcess.allowClose();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, SECONDS)).isTrue();
            handle.stop();
        }
    }

    @Test
    void sendFailureInvalidatesTheGenerationAndTriggersRecovery() throws Exception {
        Fixture fixture = new Fixture();
        QuarkusApplicationDevDeploymentHandle handle = fixture.start();
        FakeServer server = fixture.servers.get(0);
        server.sendFailure = true;

        handle.acceptReadyChangesOutcome(rebaseline(handle.nextSequence()));

        assertThat(handle.deliverReadyChangesOutcome()).isEqualTo("SEND_FAILED");
        assertThat(handle.isRunning()).isFalse();
        assertThat(fixture.triggerGeneration()).isEqualTo(1);
        assertThat(handle.acquire()).isEqualTo(QuarkusApplicationDevDeployments.Acquisition.RESTARTED_AFTER_FAILURE);
        handle.stop();
    }

    @Test
    void recoveryRebaselineRemainsPendingWhileLiveReloadIsDisabled() throws Exception {
        Fixture fixture = new Fixture();
        QuarkusApplicationDevDeploymentHandle handle = fixture.start();
        fixture.processes.get(0).exit(1);
        handle.acquire();
        FakeServer recoveredServer = fixture.servers.get(1);
        recoveredServer.status = BuildOutputChangesApplyStatus.LIVE_RELOAD_DISABLED;
        BuildOutputChanges rebaseline = rebaseline(handle.nextSequence());

        assertThat(handle.acceptReadyChangesOutcome(rebaseline)).isEqualTo("PENDING");
        assertThat(handle.deliverReadyChangesOutcome()).isEqualTo("SENT_LIVE_RELOAD_DISABLED");
        assertThat(recoveredServer.sent).endsWith(rebaseline);

        recoveredServer.liveReloadStateChanged(1, true);
        recoveredServer.status = BuildOutputChangesApplyStatus.APPLIED;

        assertThat(handle.deliverReadyChangesOutcome()).isEqualTo("SENT_APPLIED");
        assertThat(recoveredServer.sent).endsWith(rebaseline, rebaseline);
        assertThat(fixture.triggerEpoch()).isNotBlank();
        assertThat(fixture.triggerGeneration()).isEqualTo(1);
        handle.stop();
    }

    @Test
    void finalStopSuppressesFailureRecoveryAndWritesTheCloseReceiptOnlyOnce() throws Exception {
        Fixture fixture = new Fixture();
        QuarkusApplicationDevDeploymentHandle handle = fixture.start();
        FakeProcess process = fixture.processes.get(0);

        handle.stop();
        String triggerBeforeLateExit = fixture.triggerContent();
        process.exit(1);
        handle.stop();

        assertThat(fixture.triggerContent()).isEqualTo(triggerBeforeLateExit);
        assertThat(fixture.closeReceipt).hasContent("closed\n");
        assertThatThrownBy(handle::acquire)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void circuitOpensAfterThreeRecoveriesInsideTheRollingWindow() throws Exception {
        Fixture fixture = new Fixture();
        QuarkusApplicationDevDeploymentHandle handle = fixture.start();

        for (int recovery = 0; recovery < 3; recovery++) {
            fixture.processes.get(recovery).exit(recovery + 1);
            assertThat(handle.acquire()).isEqualTo(QuarkusApplicationDevDeployments.Acquisition.RESTARTED_AFTER_FAILURE);
        }
        String triggerBeforeCircuitOpens = fixture.triggerContent();
        fixture.processes.get(3).exit(4);

        assertThat(fixture.triggerContent()).isEqualTo(triggerBeforeCircuitOpens);
        assertThatThrownBy(handle::acquire)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("failed repeatedly");
        handle.stop();
    }

    @Test
    void circuitWindowResetsAfterAHealthyMinute() throws Exception {
        Fixture fixture = new Fixture();
        QuarkusApplicationDevDeploymentHandle handle = fixture.start();

        for (int recovery = 0; recovery < 3; recovery++) {
            fixture.processes.get(recovery).exit(recovery + 1);
            handle.acquire();
        }
        fixture.clock.addAndGet(Duration.ofSeconds(61).toNanos());
        fixture.processes.get(3).exit(4);

        assertThat(handle.acquire()).isEqualTo(QuarkusApplicationDevDeployments.Acquisition.RESTARTED_AFTER_FAILURE);
        handle.stop();
    }

    private static BuildOutputChanges rebaseline(long sequence) {
        return new BuildOutputChanges(sequence, BuildOutputChangeStatus.BUILD_SUCCEEDED, BuildOutputFailureKind.NONE,
                List.of(), List.of(), List.of(), List.of(), null, null, false, true,
                BuildOutputChangesDeliveryKind.REBASELINE);
    }

    private final class Fixture {

        private final Path closeReceipt = directory.resolve("session/closed.txt");
        private final Path trigger = directory.resolve("session/replay.trigger");
        private final AtomicLong clock = new AtomicLong();
        private final List<FakeServer> servers = new ArrayList<>();
        private final List<FakeProcess> processes = new ArrayList<>();

        private QuarkusApplicationDevDeploymentHandle start() {
            var handle = new QuarkusApplicationDevDeploymentHandle("fingerprint", closeReceipt, trigger,
                    ignored -> {
                        FakeProcess process = new FakeProcess();
                        processes.add(process);
                        return process;
                    },
                    stateListener -> {
                        FakeServer server = new FakeServer(stateListener);
                        servers.add(server);
                        return server;
                    },
                    clock::get);
            handle.start(null);
            return handle;
        }

        private String triggerContent() throws IOException {
            return Files.isRegularFile(trigger) ? Files.readString(trigger) : "";
        }

        private String triggerEpoch() throws IOException {
            return triggerContent().lines()
                    .filter(line -> line.startsWith("epoch="))
                    .map(line -> line.substring("epoch=".length()))
                    .findFirst()
                    .orElse("");
        }

        private long triggerGeneration() throws IOException {
            return triggerContent().lines()
                    .filter(line -> line.startsWith("generation="))
                    .map(line -> line.substring("generation=".length()))
                    .mapToLong(Long::parseLong)
                    .findFirst()
                    .orElse(0);
        }
    }

    private static final class FakeProcess implements QuarkusApplicationDevProcessHandle {

        private final CompletableFuture<Integer> exit = new CompletableFuture<>();
        private final CompletableFuture<Void> closed = new CompletableFuture<>();
        private final CountDownLatch closeStarted = new CountDownLatch(1);
        private volatile CountDownLatch closeGate;
        private volatile boolean alive = true;

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public CompletionStage<Integer> exitCode() {
            return exit;
        }

        private void exit(int code) {
            alive = false;
            exit.complete(code);
        }

        private void blockClose() {
            closeGate = new CountDownLatch(1);
        }

        private void allowClose() {
            CountDownLatch gate = closeGate;
            if (gate != null) {
                gate.countDown();
            }
        }

        @Override
        public void close() throws InterruptedException {
            closeStarted.countDown();
            CountDownLatch gate = closeGate;
            if (gate != null) {
                gate.await();
            }
            alive = false;
            exit.complete(0);
            closed.complete(null);
        }
    }

    private static final class FakeServer implements BuildOutputChangesServer {

        private final Consumer<BuildOutputLiveReloadState> stateListener;
        private final CompletableFuture<Void> termination = new CompletableFuture<>();
        private final List<BuildOutputChanges> sent = new ArrayList<>();
        private BuildOutputChangesApplyStatus status = BuildOutputChangesApplyStatus.APPLIED;
        private boolean sendFailure;

        private FakeServer(Consumer<BuildOutputLiveReloadState> stateListener) {
            this.stateListener = stateListener;
        }

        @Override
        public DevModeContext.ExternalBuildOutputTransport transport() {
            return DevModeContext.ExternalBuildOutputTransport.disabled();
        }

        @Override
        public BuildOutputChangesApplyStatus send(BuildOutputChanges changes) throws IOException {
            if (sendFailure) {
                throw new IOException("controlled send failure");
            }
            sent.add(changes);
            return status;
        }

        @Override
        public CompletionStage<Void> termination() {
            return termination;
        }

        private void fail() {
            termination.completeExceptionally(new IOException("controlled transport failure"));
        }

        private void liveReloadStateChanged(long generation, boolean enabled) {
            stateListener.accept(new BuildOutputLiveReloadState(generation, enabled));
        }

        @Override
        public void close() {
            termination.complete(null);
        }
    }
}
