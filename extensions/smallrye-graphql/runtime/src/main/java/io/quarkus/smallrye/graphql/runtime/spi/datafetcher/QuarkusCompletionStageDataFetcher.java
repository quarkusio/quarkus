package io.quarkus.smallrye.graphql.runtime.spi.datafetcher;

import java.util.List;
import java.util.concurrent.CompletionStage;

import graphql.schema.DataFetchingEnvironment;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;
import io.smallrye.context.CleanAutoCloseable;
import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.graphql.execution.context.SmallRyeContext;
import io.smallrye.graphql.execution.context.SmallRyeContextManager;
import io.smallrye.graphql.execution.datafetcher.CompletionStageDataFetcher;
import io.smallrye.graphql.schema.model.Operation;
import io.smallrye.graphql.schema.model.Type;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

public class QuarkusCompletionStageDataFetcher<K, T> extends CompletionStageDataFetcher<K, T> {

    public QuarkusCompletionStageDataFetcher(Operation operation, Type type) {
        super(operation, type);
    }

    @Override
    protected Uni<?> handleUserMethodCall(DataFetchingEnvironment dfe, Object[] transformedArguments) throws Exception {
        Context vc = Vertx.currentContext();

        if (ContextHelper.nonBlockingShouldExecuteBlocking(operation, vc)) {
            return handleUserMethodCallBlocking(dfe, transformedArguments, vc);
        } else {
            return super.handleUserMethodCall(dfe, transformedArguments);
        }
    }

    @Override
    protected Uni<List<T>> handleUserBatchLoad(DataFetchingEnvironment dfe, Object[] arguments) throws Exception {
        Context vc = Vertx.currentContext();
        ContextHelper.getActiveState(dfe);
        if (ContextHelper.nonBlockingShouldExecuteBlocking(operation, vc)) {
            return handleUserBatchLoadBlocking(dfe, arguments, vc);
        } else {
            return super.handleUserBatchLoad(dfe, arguments);
        }
    }

    private Uni<?> handleUserMethodCallBlocking(DataFetchingEnvironment dfe, Object[] transformedArguments, Context vc)
            throws Exception {
        final SmallRyeContext smallRyeContext = SmallRyeContextManager.getCurrentSmallRyeContext();
        final InjectableContext.ContextState requestContextState = ContextHelper.getActiveState(dfe);
        final ManagedContext requestContext = Arc.container().requestContext();

        final Promise<Object> result = Promise.promise();
        SmallRyeThreadContext threadContext = SmallRyeThreadContext.builder().propagated(SmallRyeThreadContext.ALL_REMAINING)
                .cleared().unchanged().build();
        try (CleanAutoCloseable ac = SmallRyeThreadContext.withThreadContext(threadContext)) {
            vc.executeBlocking(future -> {
                boolean shouldActivate = ContextHelper.shouldReactivateRequestContext(requestContext, requestContextState);
                try {
                    if (shouldActivate) {
                        requestContext.activate(requestContextState);
                    }
                    SmallRyeContextManager.restore(smallRyeContext);
                    CompletionStage<?> resultFromMethodCall = (CompletionStage<?>) operationInvoker
                            .invoke(transformedArguments);
                    Object objectFromMethodCall = resultFromMethodCall.toCompletableFuture().get();
                    future.complete(objectFromMethodCall);
                } catch (Exception ex) {
                    future.fail(ex);
                } finally {
                    SmallRyeContextManager.clearCurrentSmallRyeContext();
                    ContextHelper.storeActiveState(dfe, requestContext.getState());
                    if (shouldActivate) {
                        requestContext.deactivate();
                    }
                }
            }, result);
        }
        CompletionStage<Object> withContextCapture = threadContext.withContextCapture(result.future().toCompletionStage());

        return Uni.createFrom().completionStage(withContextCapture);
    }

    private Uni<List<T>> handleUserBatchLoadBlocking(DataFetchingEnvironment dfe, Object[] arguments, Context vc)
            throws Exception {

        final SmallRyeContext smallRyeContext = SmallRyeContextManager.getCurrentSmallRyeContext();
        final InjectableContext.ContextState requestContextState = ContextHelper.getActiveState(dfe);
        final ManagedContext requestContext = Arc.container().requestContext();

        final Promise<List<T>> result = Promise.promise();
        SmallRyeThreadContext threadContext = SmallRyeThreadContext.builder().propagated(SmallRyeThreadContext.ALL_REMAINING)
                .cleared().unchanged().build();
        try (CleanAutoCloseable ac = SmallRyeThreadContext.withThreadContext(threadContext)) {
            vc.executeBlocking(future -> {
                boolean shouldActivate = ContextHelper.shouldReactivateRequestContext(requestContext, requestContextState);
                try {
                    if (shouldActivate) {
                        requestContext.activate(requestContextState);
                    }
                    SmallRyeContextManager.restore(smallRyeContext);
                    @SuppressWarnings("unchecked")
                    CompletionStage<List<T>> resultFromMethodCall = (CompletionStage<List<T>>) operationInvoker
                            .invoke(arguments);
                    List<T> objectFromMethodCall = resultFromMethodCall.toCompletableFuture().get();
                    future.complete(objectFromMethodCall);
                } catch (Exception ex) {
                    future.fail(ex);
                } finally {
                    SmallRyeContextManager.clearCurrentSmallRyeContext();
                    ContextHelper.storeActiveState(dfe, requestContext.getState());
                    if (shouldActivate) {
                        requestContext.deactivate();
                    }
                }
            }, result);
        }
        CompletionStage<List<T>> withContextCapture = threadContext.withContextCapture(result.future().toCompletionStage());
        return Uni.createFrom().completionStage(withContextCapture);
    }

}
