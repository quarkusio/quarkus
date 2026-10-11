package io.quarkus.grpc.context;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.context.proto.CheckRequest;
import io.quarkus.grpc.context.proto.ContextCheckGrpc;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Cancellation tests for the gRPC context lifecycle, kept in a separate class so that
 * the slow handlers required to trigger onCancel() do not leave stale state that
 * interferes with the close()-path tests in {@link GrpcContextLifecycleTest}.
 */
public class GrpcContextCancelLifecycleTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(ContextCheckGrpc.class.getPackage())
                    .addClasses(ContextCheckService.class, ContextWatcherInterceptor.class,
                            ContextWatcherInterceptor.Outer.class, ContextWatcherInterceptor.Inner.class))
            .withConfigurationResource("context-check-config.properties");

    @GrpcClient("context-check")
    ContextCheckGrpc.ContextCheckBlockingStub stub;

    @Inject
    ContextWatcherInterceptor watcher;

    @BeforeEach
    void resetWatcher() {
        watcher.reset();
    }

    // -------------------------------------------------------------------------
    // Context cleaned up via onCancel() when the client cancels before the server responds
    // -------------------------------------------------------------------------

    @Test
    void grpcContextIsCleanedUpAfterCancelOnBlockingWorker() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // Issue a call with a short deadline; the server holds the call open so it gets cancelled.
            executor.submit(() -> {
                try {
                    stub.withDeadlineAfter(300, TimeUnit.MILLISECONDS)
                            .checkSlowBlocking(CheckRequest.getDefaultInstance());
                } catch (StatusRuntimeException ignored) {
                }
            });

            // Poll until onCancel() cleanup has run on the duplicated context.
            // There is no single reliable hook that fires after cleanup on all paths, so we
            // let Awaitility drive the determinism instead of a hand-rolled latch.
            await().atMost(5, SECONDS)
                    .alias("gRPC context must be ROOT on the duplicated context after cancel on the blocking-worker path")
                    .until(() -> watcher.readContextOnDuplicatedContext().get(1, SECONDS) == io.grpc.Context.ROOT);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void grpcContextIsCleanedUpAfterCancelOnVirtualThread() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            executor.submit(() -> {
                try {
                    stub.withDeadlineAfter(300, TimeUnit.MILLISECONDS)
                            .checkSlowVirtual(CheckRequest.getDefaultInstance());
                } catch (StatusRuntimeException ignored) {
                }
            });

            await().atMost(5, SECONDS)
                    .alias("gRPC context must be ROOT on the duplicated context after cancel on the virtual-thread path")
                    .until(() -> watcher.readContextOnDuplicatedContext().get(1, SECONDS) == io.grpc.Context.ROOT);
        } finally {
            executor.shutdownNow();
        }
    }
}
