package io.quarkus.smallrye.graphql.runtime.spi.datafetcher;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.graphql.GraphQLException;

import graphql.execution.AbortExecutionException;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.quarkus.arc.Arc;
import io.smallrye.context.SmallRyeThreadContext;
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
        if (runBlocking(dfe) || BlockingHelper.blockingShouldExecuteNonBlocking(operation, vc)) {
            return super.invokeAndTransform(dfe, resultBuilder, transformedArguments);
        } else {
            return invokeAndTransformBlocking(dfe, resultBuilder, transformedArguments, vc);
        }
    }

    @Override
    public CompletionStage<List<T>> invokeBatch(DataFetchingEnvironment dfe, Object[] arguments) {

        Context vc = Vertx.currentContext();
        if (runBlocking(dfe) || BlockingHelper.blockingShouldExecuteNonBlocking(operation, vc)) {
            return super.invokeBatch(dfe, arguments);
        } else {
            return invokeBatchBlocking(arguments, vc);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T invokeAndTransformBlocking(final DataFetchingEnvironment dfe, DataFetcherResult.Builder<Object> resultBuilder,
            Object[] transformedArguments, Context vc) throws Exception {

        SmallRyeThreadContext threadContext = Arc.container().select(SmallRyeThreadContext.class).get();
        final Promise<T> result = Promise.promise();

        // We need some make sure that we call given the context
        @SuppressWarnings("unchecked")
        Callable<Object> contextualCallable = threadContext.contextualCallable(() -> {
            try {
                Object resultFromMethodCall = operationInvoker.invoke(transformedArguments);
                Object resultFromTransform = fieldHelper.transformOrAdaptResponse(resultFromMethodCall, dfe);
                resultBuilder.data(resultFromTransform);
                return (T) resultBuilder.build();
            } catch (AbstractDataFetcherException te) {
                te.appendDataFetcherResult(resultBuilder, dfe);
                return (T) resultBuilder.build();
            } catch (GraphQLException graphQLException) {
                errorResultHelper.appendPartialResult(resultBuilder, dfe, graphQLException);
                return (T) resultBuilder.build();
            } catch (Error e) {
                resultBuilder.clearErrors().data(null).error(new AbortExecutionException(e));
                return (T) resultBuilder.build();
            }
        });

        // Here call blocking with context
        BlockingHelper.runBlocking(vc, contextualCallable, result);
        return (T) result.future().toCompletionStage();
    }

    @SuppressWarnings("unchecked")
    private CompletionStage<List<T>> invokeBatchBlocking(Object[] arguments, Context vc) {
        SmallRyeThreadContext threadContext = Arc.container().select(SmallRyeThreadContext.class).get();
        final Promise<List<T>> result = Promise.promise();

        // We need some make sure that we call given the context
        Callable<Object> contextualCallable = threadContext.contextualCallable(() -> {
            return (List<T>) operationInvoker.invokePrivileged(arguments);
        });

        // Here call blocking with context
        BlockingHelper.runBlocking(vc, contextualCallable, result);
        return result.future().toCompletionStage();
    }

    private boolean runBlocking(DataFetchingEnvironment dfe) {
        return dfe.getGraphQlContext().get("runBlocking");
    }
}
