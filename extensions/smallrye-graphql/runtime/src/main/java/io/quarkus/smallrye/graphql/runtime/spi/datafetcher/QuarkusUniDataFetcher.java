package io.quarkus.smallrye.graphql.runtime.spi.datafetcher;

import java.util.List;
import java.util.concurrent.Callable;

import graphql.schema.DataFetchingEnvironment;
import io.quarkus.arc.Arc;
import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.graphql.schema.model.Operation;
import io.smallrye.graphql.schema.model.Type;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

public class QuarkusUniDataFetcher<K, T> extends AbstractAsyncDataFetcher<K, T> {

    public QuarkusUniDataFetcher(Operation operation, Type type) {
        super(operation, type);
    }

    @Override
    protected Uni<?> handleUserMethodCall(DataFetchingEnvironment dfe, Object[] transformedArguments) throws Exception {
        Context vc = Vertx.currentContext();
        if (runBlocking(dfe) || !BlockingHelper.nonBlockingShouldExecuteBlocking(operation, vc)) {
            return handleUserMethodCallNonBlocking(transformedArguments);
        } else {
            return handleUserMethodCallBlocking(transformedArguments, vc);
        }
    }

    @Override
    protected Uni<List<T>> handleUserBatchLoad(DataFetchingEnvironment dfe, Object[] arguments) throws Exception {
        Context vc = Vertx.currentContext();
        if (runBlocking(dfe) || !BlockingHelper.nonBlockingShouldExecuteBlocking(operation, vc)) {
            return handleUserBatchLoadNonBlocking(arguments);
        } else {
            return handleUserBatchLoadBlocking(arguments, vc);
        }
    }

    private Uni<?> handleUserMethodCallNonBlocking(final Object[] transformedArguments)
            throws Exception {
        return (Uni<?>) operationInvoker.invoke(transformedArguments);
    }

    private Uni<?> handleUserMethodCallBlocking(Object[] transformedArguments, Context vc)
            throws Exception {

        SmallRyeThreadContext threadContext = Arc.container().select(SmallRyeThreadContext.class).get();
        final Promise<T> result = Promise.promise();

        // We need some make sure that we call given the context
        Callable<Object> contextualCallable = threadContext.contextualCallable(() -> {
            Object resultFromMethodCall = operationInvoker.invoke(transformedArguments);
            Uni<?> uniFromMethodCall = (Uni<?>) resultFromMethodCall;
            return uniFromMethodCall.subscribeAsCompletionStage().get();
        });

        // Here call blocking with context
        BlockingHelper.runBlocking(vc, contextualCallable, result, operation);
        return Uni.createFrom().completionStage(result.future().toCompletionStage());
    }

    @SuppressWarnings("unchecked")
    protected Uni<List<T>> handleUserBatchLoadNonBlocking(final Object[] arguments)
            throws Exception {
        return ((Uni<List<T>>) operationInvoker.invoke(arguments));
    }

    private Uni<List<T>> handleUserBatchLoadBlocking(Object[] arguments, Context vc)
            throws Exception {

        SmallRyeThreadContext threadContext = Arc.container().select(SmallRyeThreadContext.class).get();
        final Promise<List<T>> result = Promise.promise();

        // We need some make sure that we call given the context
        Callable<Object> contextualCallable = threadContext.contextualCallable(() -> {
            Object resultFromMethodCall = operationInvoker.invoke(arguments);
            @SuppressWarnings("unchecked")
            Uni<List<T>> uniFromMethodCall = (Uni<List<T>>) resultFromMethodCall;
            return uniFromMethodCall.subscribeAsCompletionStage().get();
        });

        // Here call blocking with context
        BlockingHelper.runBlocking(vc, contextualCallable, result, operation);
        return Uni.createFrom().completionStage(result.future().toCompletionStage());
    }

    private boolean runBlocking(DataFetchingEnvironment dfe) {
        return dfe.getGraphQlContext().get("runBlocking");
    }
}
