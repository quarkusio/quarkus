package io.quarkus.grpc.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.context.proto.CheckRequest;
import io.quarkus.grpc.context.proto.ContextCheckGrpc;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Integration tests verifying that the gRPC context is correctly attached and cleaned up
 * on all execution paths (event-loop, blocking worker, virtual thread).
 */
public class GrpcContextLifecycleTest {

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
    // Context available during service-method execution
    // -------------------------------------------------------------------------

    @Test
    void grpcContextIsAvailableDuringEventLoopHandlerExecution() {
        assertThat(stub.check(CheckRequest.getDefaultInstance()).getContextState())
                .as("gRPC context must be non-ROOT when the event-loop service method executes")
                .isEqualTo("ok");
    }

    @Test
    void grpcContextIsAvailableDuringBlockingHandlerExecution() {
        assertThat(stub.checkBlocking(CheckRequest.getDefaultInstance()).getContextState())
                .as("gRPC context must be non-ROOT in a @Blocking handler even after a delay")
                .isEqualTo("ok");
    }

    @Test
    void grpcContextIsAvailableDuringVirtualThreadHandlerExecution() {
        assertThat(stub.checkVirtual(CheckRequest.getDefaultInstance()).getContextState())
                .as("gRPC context must be non-ROOT in a @RunOnVirtualThread handler even after a delay")
                .isEqualTo("ok");
    }

    // -------------------------------------------------------------------------
    // Context available at the listener layer — onMessage (ContextWatcherInterceptor)
    // -------------------------------------------------------------------------

    @Test
    void grpcContextIsAttachedInListenerOnMessageOnEventLoop() {
        stub.check(CheckRequest.getDefaultInstance());

        assertThat(watcher.getContextAtOnMessage())
                .as("gRPC context must be non-ROOT in onMessage on the event-loop path")
                .isNotNull()
                .isNotSameAs(io.grpc.Context.ROOT);
    }

    @Test
    void grpcContextIsAttachedInListenerOnMessageOnBlockingWorker() {
        stub.checkBlocking(CheckRequest.getDefaultInstance());

        assertThat(watcher.getContextAtOnMessage())
                .as("gRPC context must be non-ROOT in onMessage on the blocking-worker path")
                .isNotNull()
                .isNotSameAs(io.grpc.Context.ROOT);
    }

    @Test
    void grpcContextIsAttachedInListenerOnMessageOnVirtualThread() {
        stub.checkVirtual(CheckRequest.getDefaultInstance());

        assertThat(watcher.getContextAtOnMessage())
                .as("gRPC context must be non-ROOT in onMessage on the virtual-thread path")
                .isNotNull()
                .isNotSameAs(io.grpc.Context.ROOT);
    }

    // -------------------------------------------------------------------------
    // Context available at the listener layer — onHalfClose (ContextWatcherInterceptor)
    // -------------------------------------------------------------------------

    @Test
    void grpcContextIsAttachedInListenerOnHalfCloseOnEventLoop() {
        stub.check(CheckRequest.getDefaultInstance());

        assertThat(watcher.getContextAtOnHalfClose())
                .as("gRPC context must be non-ROOT in onHalfClose on the event-loop path")
                .isNotNull()
                .isNotSameAs(io.grpc.Context.ROOT);
    }

    @Test
    void grpcContextIsAttachedInListenerOnHalfCloseOnBlockingWorker() {
        stub.checkBlocking(CheckRequest.getDefaultInstance());

        assertThat(watcher.getContextAtOnHalfClose())
                .as("gRPC context must be non-ROOT in onHalfClose on the blocking-worker path")
                .isNotNull()
                .isNotSameAs(io.grpc.Context.ROOT);
    }

    @Test
    void grpcContextIsAttachedInListenerOnHalfCloseOnVirtualThread() {
        stub.checkVirtual(CheckRequest.getDefaultInstance());

        assertThat(watcher.getContextAtOnHalfClose())
                .as("gRPC context must be non-ROOT in onHalfClose on the virtual-thread path")
                .isNotNull()
                .isNotSameAs(io.grpc.Context.ROOT);
    }

    // -------------------------------------------------------------------------
    // Context cleaned up from the duplicated Vert.x context after close()
    // -------------------------------------------------------------------------

    @Test
    void grpcContextIsCleanedUpAfterCloseOnEventLoop() throws Exception {
        stub.check(CheckRequest.getDefaultInstance());

        assertThat(watcher.awaitClose(5, TimeUnit.SECONDS))
                .as("close() must have fired within 5s on the event-loop path")
                .isTrue();

        assertThat(watcher.readContextOnDuplicatedContext().get(5, TimeUnit.SECONDS))
                .as("gRPC context must be ROOT on the duplicated context after close() on the event-loop path")
                .isSameAs(io.grpc.Context.ROOT);
    }

    @Test
    void grpcContextIsCleanedUpAfterCloseOnBlockingWorker() throws Exception {
        stub.checkBlocking(CheckRequest.getDefaultInstance());

        assertThat(watcher.awaitClose(5, TimeUnit.SECONDS))
                .as("close() must have fired within 5s on the blocking-worker path")
                .isTrue();

        assertThat(watcher.readContextOnDuplicatedContext().get(5, TimeUnit.SECONDS))
                .as("gRPC context must be ROOT on the duplicated context after close() on the blocking-worker path")
                .isSameAs(io.grpc.Context.ROOT);
    }

    @Test
    void grpcContextIsCleanedUpAfterCloseOnVirtualThread() throws Exception {
        stub.checkVirtual(CheckRequest.getDefaultInstance());

        assertThat(watcher.awaitClose(5, TimeUnit.SECONDS))
                .as("close() must have fired within 5s on the virtual-thread path")
                .isTrue();

        assertThat(watcher.readContextOnDuplicatedContext().get(5, TimeUnit.SECONDS))
                .as("gRPC context must be ROOT on the duplicated context after close() on the virtual-thread path")
                .isSameAs(io.grpc.Context.ROOT);
    }

    // -------------------------------------------------------------------------
    // Context consistent across sequential calls (regression for stale-context leaks)
    // -------------------------------------------------------------------------

    @Test
    void grpcContextIsCorrectAcrossSequentialCalls() {
        for (int i = 0; i < 3; i++) {
            watcher.reset();
            assertThat(stub.check(CheckRequest.getDefaultInstance()).getContextState())
                    .as("gRPC context must be non-ROOT on sequential call %d", i)
                    .isEqualTo("ok");
        }
    }
}
