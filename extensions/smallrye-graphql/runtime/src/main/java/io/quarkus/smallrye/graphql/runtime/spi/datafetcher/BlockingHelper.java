package io.quarkus.smallrye.graphql.runtime.spi.datafetcher;

import java.util.concurrent.Callable;

import io.smallrye.graphql.schema.model.Execute;
import io.smallrye.graphql.schema.model.Operation;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

public final class BlockingHelper {

    private BlockingHelper() {
    }

    public static boolean blockingShouldExecuteNonBlocking(Operation operation) {
        // Rule is that by default this should execute blocking except if marked as non-blocking)
        return operation.getExecute().equals(Execute.NON_BLOCKING);
    }

    public static boolean nonBlockingShouldExecuteBlocking(Operation operation) {
        // Rule is that by default this should execute non-blocking except if marked as blocking
        return operation.getExecute().equals(Execute.BLOCKING) && Vertx.currentContext().isEventLoopContext();
    }

    @SuppressWarnings("unchecked")
    public static void runBlocking(Callable<Object> contextualCallable, Promise result) {
        // Here call blocking with context
        Uni.createFrom().item(contextualCallable)
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .subscribe().with(r -> {
                    try {
                        result.complete(r.call());
                    } catch (Exception ex) {
                        result.fail(ex);
                    }
                });
    }
}
