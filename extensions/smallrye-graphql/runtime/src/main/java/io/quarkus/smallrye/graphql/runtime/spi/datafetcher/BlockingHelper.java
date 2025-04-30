package io.quarkus.smallrye.graphql.runtime.spi.datafetcher;

import java.util.concurrent.Callable;

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

    @SuppressWarnings("unchecked")
    public static void runBlocking(Context vc, Callable<Object> contextualCallable, Promise result) {
        // Here call blocking with context
        vc.executeBlocking(contextualCallable).onComplete(result);
    }
}
