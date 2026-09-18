package io.quarkus.grpc.context;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.CountDownLatch;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.context.proto.CheckReply;
import io.quarkus.grpc.context.proto.CheckRequest;
import io.quarkus.grpc.context.proto.ContextCheckGrpc;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Cancellation tests for the gRPC context lifecycle, kept in a separate class so that
 * the slow handlers required to trigger cancellation do not leave stale state that
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
    Channel channel;

    @Inject
    ContextWatcherInterceptor watcher;

    @BeforeEach
    void resetWatcher() {
        watcher.reset();
    }

    // -------------------------------------------------------------------------
    // Context cleaned up when the client cancels before the server responds
    // -------------------------------------------------------------------------

    @Test
    void grpcContextIsCleanedUpAfterCancelOnBlockingWorker() throws Exception {
        CountDownLatch done = cancelCall(ContextCheckGrpc.getCheckSlowBlockingMethod());

        await().atMost(5, SECONDS)
                .alias("Inner.onCancel() must fire on the blocking-worker path")
                .until(() -> watcher.getContextAtOnCancel() != null);
        assertThat(watcher.getContextAtOnCancel())
                .as("gRPC context must be non-ROOT during onCancel() on the blocking-worker path\n"
                        + "  onDuplicatedCtx=" + watcher.onCancelOnDuplicatedContext
                        + "  vertxCtx=" + watcher.onCancelVertxContext
                        + "  thread=" + watcher.onCancelThread)
                .isNotSameAs(io.grpc.Context.ROOT);
        await().atMost(5, SECONDS)
                .alias("gRPC context must be ROOT on the duplicated context after cancel on the blocking-worker path")
                .until(() -> watcher.readContextOnDuplicatedContext().get(1, SECONDS) == io.grpc.Context.ROOT);

        done.await(5, SECONDS);
    }

    @Test
    void grpcContextIsCleanedUpAfterCancelOnVirtualThread() throws Exception {
        CountDownLatch done = cancelCall(ContextCheckGrpc.getCheckSlowVirtualMethod());

        await().atMost(5, SECONDS)
                .alias("Inner.onCancel() must fire on the virtual-thread path")
                .until(() -> watcher.getContextAtOnCancel() != null);
        assertThat(watcher.getContextAtOnCancel())
                .as("gRPC context must be non-ROOT during onCancel() on the virtual-thread path\n"
                        + "  onDuplicatedCtx=" + watcher.onCancelOnDuplicatedContext
                        + "  vertxCtx=" + watcher.onCancelVertxContext
                        + "  thread=" + watcher.onCancelThread)
                .isNotSameAs(io.grpc.Context.ROOT);
        await().atMost(5, SECONDS)
                .alias("gRPC context must be ROOT on the duplicated context after cancel on the virtual-thread path")
                .until(() -> watcher.readContextOnDuplicatedContext().get(1, SECONDS) == io.grpc.Context.ROOT);

        done.await(5, SECONDS);
    }

    @Test
    void grpcContextIsCleanedUpAfterCancelOnEventLoop() throws Exception {
        CountDownLatch done = cancelCall(ContextCheckGrpc.getCheckSlowEventLoopMethod());

        await().atMost(5, SECONDS)
                .alias("Inner.onCancel() must fire on the event-loop path")
                .until(() -> watcher.getContextAtOnCancel() != null);
        assertThat(watcher.getContextAtOnCancel())
                .as("gRPC context must be non-ROOT during onCancel() on the event-loop path\n"
                        + "  onDuplicatedCtx=" + watcher.onCancelOnDuplicatedContext
                        + "  vertxCtx=" + watcher.onCancelVertxContext
                        + "  thread=" + watcher.onCancelThread)
                .isNotSameAs(io.grpc.Context.ROOT);
        await().atMost(5, SECONDS)
                .alias("gRPC context must be ROOT on the duplicated context after cancel on the event-loop path")
                .until(() -> watcher.readContextOnDuplicatedContext().get(1, SECONDS) == io.grpc.Context.ROOT);

        done.await(5, SECONDS);
    }

    /**
     * Starts a call for the given method, waits 150 ms for the server handler to reach a stable
     * state, then sends RST_STREAM via {@code ClientCall.cancel()}. Using cancel() rather than
     * {@code stub.withDeadlineAfter()} avoids creating a {@code CancellableContext} on the server,
     * which would race with the RST_STREAM error handler and prevent {@code onCancel()} from firing.
     *
     * @return a latch that is released when the call closes on the client side
     */
    private CountDownLatch cancelCall(MethodDescriptor<CheckRequest, CheckReply> method)
            throws InterruptedException {
        ClientCall<CheckRequest, CheckReply> call = channel.newCall(method, CallOptions.DEFAULT);
        CountDownLatch done = new CountDownLatch(1);
        call.start(new ClientCall.Listener<>() {
            @Override
            public void onClose(Status status, Metadata trailers) {
                done.countDown();
            }
        }, new Metadata());
        call.request(1);
        call.sendMessage(CheckRequest.getDefaultInstance());
        call.halfClose();
        Thread.sleep(150); // give server time to reach a stable state before cancellation
        call.cancel("test cancel", null);
        return done;
    }
}
