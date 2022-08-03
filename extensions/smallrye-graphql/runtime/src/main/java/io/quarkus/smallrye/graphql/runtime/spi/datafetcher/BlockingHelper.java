package io.quarkus.smallrye.graphql.runtime.spi.datafetcher;

import java.util.concurrent.Callable;

import graphql.schema.DataFetchingEnvironment;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;
import io.smallrye.graphql.schema.model.Execute;
import io.smallrye.graphql.schema.model.Operation;
import io.vertx.core.Context;
import io.vertx.core.Promise;

public class BlockingHelper {

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
        vc.executeBlocking(future -> {
            try {
                future.complete(contextualCallable.call());
            } catch (Exception ex) {
                future.fail(ex);
            }
        }, result);
    }

    public static void reactivate(ManagedContext requestContext, DataFetchingEnvironment dfe) {
        if (!requestContext.isActive()) {
            Object maybeState = dfe.getGraphQlContext().getOrDefault("state", null);
            if (maybeState != null) {
                requestContext.activate((InjectableContext.ContextState) maybeState);
            } else {
                requestContext.activate();
            }
        }
    }
}
