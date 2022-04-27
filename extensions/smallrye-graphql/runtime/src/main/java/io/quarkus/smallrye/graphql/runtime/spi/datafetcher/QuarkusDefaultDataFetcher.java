package io.quarkus.smallrye.graphql.runtime.spi.datafetcher;

import java.util.List;
import java.util.concurrent.CompletionStage;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;
import io.smallrye.context.CleanAutoCloseable;
import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.graphql.execution.context.SmallRyeContext;
import io.smallrye.graphql.execution.context.SmallRyeContextManager;
import io.smallrye.graphql.execution.datafetcher.DefaultDataFetcher;
import io.smallrye.graphql.schema.model.Operation;
import io.smallrye.graphql.schema.model.Type;
import io.smallrye.graphql.transformation.AbstractDataFetcherException;
import io.vertx.core.Context;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

public class QuarkusDefaultDataFetcher<K, T> extends DefaultDataFetcher<K, T> {

    public QuarkusDefaultDataFetcher(Operation operation, Type type) {
        super(operation, type);
    }

    @Override
    public <T> T invokeAndTransform(DataFetchingEnvironment dfe, DataFetcherResult.Builder<Object> resultBuilder,
            Object[] transformedArguments) throws Exception {

        Context vc = Vertx.currentContext();
        if (ContextHelper.blockingShouldExecuteNonBlocking(operation, vc)) {
            return super.invokeAndTransform(dfe, resultBuilder, transformedArguments);
        } else {
            return invokeAndTransformBlocking(dfe, resultBuilder, transformedArguments, vc);
        }
    }

    @Override
    public CompletionStage<List<T>> invokeBatch(DataFetchingEnvironment dfe, Object[] arguments) {

        Context vc = Vertx.currentContext();
        if (ContextHelper.blockingShouldExecuteNonBlocking(operation, vc)) {
            return super.invokeBatch(dfe, arguments);
        } else {
            return invokeBatchBlocking(dfe, arguments, vc);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T invokeAndTransformBlocking(DataFetchingEnvironment dfe, DataFetcherResult.Builder<Object> resultBuilder,
            Object[] transformedArguments, Context vc) throws Exception {

        final SmallRyeContext smallRyeContext = SmallRyeContextManager.getCurrentSmallRyeContext();
        final InjectableContext.ContextState requestContextState = ContextHelper.getActiveState(dfe);
        final ManagedContext requestContext = Arc.container().requestContext();

        final Promise<T> result = Promise.promise();
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

                    Object resultFromMethodCall = operationInvoker.invoke(transformedArguments);
                    Object resultFromTransform = fieldHelper.transformOrAdaptResponse(resultFromMethodCall, dfe);
                    resultBuilder.data(resultFromTransform);
                    future.complete((T) resultBuilder.build());
                } catch (AbstractDataFetcherException te) {
                    te.appendDataFetcherResult(resultBuilder, dfe);
                    future.complete((T) resultBuilder.build());
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
        return (T) threadContext.withContextCapture(result.future().toCompletionStage());
    }

    @SuppressWarnings("unchecked")
    private CompletionStage<List<T>> invokeBatchBlocking(DataFetchingEnvironment dfe, Object[] arguments, Context vc) {

        final SmallRyeContext smallRyeContext = SmallRyeContextManager.getCurrentSmallRyeContext();
        final InjectableContext.ContextState requestContextState = ContextHelper.getActiveState(dfe);

        ManagedContext requestContext = Arc.container().requestContext();
        SmallRyeThreadContext threadContext = SmallRyeThreadContext.builder().propagated(SmallRyeThreadContext.ALL_REMAINING)
                .cleared().unchanged().build();
        final Promise<List<T>> result = Promise.promise();
        try (CleanAutoCloseable ac = SmallRyeThreadContext.withThreadContext(threadContext)) {
            vc.executeBlocking(future -> {
                boolean shouldActivate = ContextHelper.shouldReactivateRequestContext(requestContext, requestContextState);
                try {
                    if (shouldActivate) {
                        requestContext.activate(requestContextState);
                    }
                    SmallRyeContextManager.restore(smallRyeContext);
                    future.complete((List<T>) operationInvoker.invokePrivileged(arguments));
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
        return result.future().toCompletionStage();
    }
}
