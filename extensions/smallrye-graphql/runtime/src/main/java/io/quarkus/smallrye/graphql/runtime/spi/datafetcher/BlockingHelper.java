package io.quarkus.smallrye.graphql.runtime.spi.datafetcher;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import io.quarkus.virtual.threads.VirtualThreadsRecorder;
import io.smallrye.graphql.schema.model.Execute;
import io.smallrye.graphql.schema.model.Operation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Context;
import io.vertx.core.Promise;

public final class BlockingHelper {

    private BlockingHelper() {
    }

    public static boolean blockingShouldExecuteNonBlocking(Operation operation, Context vc) {
        // Rule is that by default this should execute blocking except if marked as non-blocking)
        return operation.getExecute().equals(Execute.NON_BLOCKING);
    }

    public static boolean nonBlockingShouldExecuteBlocking(Operation operation, Context vc) {
        // Rule is that by default this should execute non-blocking except if marked as blocking
        if (!operation.getExecute().equals(Execute.BLOCKING)) {
            return false;
        }
        return vc != null ? vc.isEventLoopContext() : Context.isOnEventLoopThread();
    }

    public static boolean shouldUseVirtualThread(Operation operation, Context vc) {
        // Use virtual threads if the operation is marked as run on virtual thread
        return operation.getExecute().equals(Execute.RUN_ON_VIRTUAL_THREAD);
    }

    @SuppressWarnings("unchecked")
    public static void runBlocking(Context vc, Callable<Object> contextualCallable, Promise result, Operation operation) {
        // Check if we should use virtual threads
        if (shouldUseVirtualThread(operation, vc)) {
            ExecutorService virtualThreadsExecutor = VirtualThreadsRecorder.getCurrent();
            virtualThreadsExecutor.submit(() -> {
                try {
                    Object value = contextualCallable.call();
                    result.complete(value);
                } catch (Throwable t) {
                    result.fail(t);
                }
            });
        } else if (vc != null) {
            vc.executeBlocking(contextualCallable, false).onComplete(result);
        } else if (Context.isOnEventLoopThread()) {
            // Event-loop thread with no Vert.x context (Netty callback that was not dispatched
            // with runOnContext). Running inline trips BlockingOperationControl.
            Infrastructure.getDefaultWorkerPool().execute(() -> {
                try {
                    result.complete(contextualCallable.call());
                } catch (Throwable t) {
                    result.fail(t);
                }
            });
        } else {
            // No Vert.x context (e.g. ForkJoinPool thread after CompletableFuture.supplyAsync) —
            // already off the event loop, safe to execute directly
            try {
                result.complete(contextualCallable.call());
            } catch (Throwable t) {
                result.fail(t);
            }
        }
    }
}
