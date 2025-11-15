package io.quarkus.smallrye.graphql.runtime.spi.datafetcher;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import io.quarkus.virtual.threads.VirtualThreadsRecorder;
import io.smallrye.graphql.schema.model.Execute;
import io.smallrye.graphql.schema.model.Operation;
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
        return operation.getExecute().equals(Execute.BLOCKING) && vc.isEventLoopContext();
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
        } else {
            // use regular blocking execution
            vc.executeBlocking(contextualCallable).onComplete(result);
        }
    }
}
