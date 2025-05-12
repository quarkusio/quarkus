package io.quarkus.smallrye.graphql.runtime.spi.datafetcher;

import io.quarkus.arc.Arc;
import io.quarkus.virtual.threads.VirtualThreads;
import io.smallrye.graphql.schema.model.Execute;
import io.smallrye.graphql.schema.model.Operation;
import io.vertx.core.Context;
import io.vertx.core.Promise;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public final class BlockingHelper {

    private static Boolean isVirtualThreadsExecutorAvailable = null; //todo or no need to cache this?


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
        // Use virtual threads if the operation is marked as blocking and we're on an event loop
        // and the virtual threads executor is available
        return isVirtualThreadsExecutorAvailable() && operation.getExecute().equals(Execute.RUN_ON_VIRTUAL_THREAD);
    }

    private static boolean isVirtualThreadsExecutorAvailable() {
        if (isVirtualThreadsExecutorAvailable == null) {
            try {
                isVirtualThreadsExecutorAvailable = Arc.container()
                        .instance(ExecutorService.class, VirtualThreads.Literal.INSTANCE).isAvailable();
            } catch (Exception e) {
                isVirtualThreadsExecutorAvailable = false;
            }
        }
        return isVirtualThreadsExecutorAvailable;
    }

    @SuppressWarnings("unchecked")
    public static void runBlocking(Context vc, Callable<Object> contextualCallable, Promise result, Operation operation) {
        // Check if we should use virtual threads
        if (shouldUseVirtualThread(operation, vc)) {
            ExecutorService virtualThreadsExecutor = Arc.container()
                    .instance(ExecutorService.class, VirtualThreads.Literal.INSTANCE).get();
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
