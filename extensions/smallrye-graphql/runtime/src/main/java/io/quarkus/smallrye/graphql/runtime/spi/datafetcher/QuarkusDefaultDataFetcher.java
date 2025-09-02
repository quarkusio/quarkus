package io.quarkus.smallrye.graphql.runtime.spi.datafetcher;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import jakarta.validation.ConstraintViolationException;

import org.eclipse.microprofile.graphql.GraphQLException;

import graphql.execution.AbortExecutionException;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.graphql.execution.context.SmallRyeContextManager;
import io.smallrye.graphql.execution.datafetcher.DefaultDataFetcher;
import io.smallrye.graphql.schema.model.Operation;
import io.smallrye.graphql.schema.model.Type;
import io.smallrye.graphql.transformation.AbstractDataFetcherException;
import io.smallrye.graphql.validation.BeanValidationUtil;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

public class QuarkusDefaultDataFetcher<K, T> extends DefaultDataFetcher<K, T> {

    public QuarkusDefaultDataFetcher(Operation operation, Type type) {
        super(operation, type);
    }

    @Override
    public <T> T invokeAndTransform(io.smallrye.graphql.api.Context c, DataFetchingEnvironment dfe,
            DataFetcherResult.Builder<Object> resultBuilder,
            Object[] transformedArguments) throws Exception {

        ManagedContext requestContext = Arc.container().requestContext();
        try {
            RequestContextHelper.reactivate(requestContext, dfe);
            Context vc = Vertx.currentContext();
            if (runBlocking(dfe) || BlockingHelper.blockingShouldExecuteNonBlocking(operation, vc)) {
                return super.invokeAndTransform(c, dfe, resultBuilder, transformedArguments);
            } else {
                return invokeAndTransformBlocking(c, dfe, resultBuilder, transformedArguments, vc);
            }
        } finally {
            deactivate(requestContext);
        }
    }

    @Override
    public CompletionStage<List<T>> invokeBatch(DataFetchingEnvironment dfe, Object[] arguments) {

        ManagedContext requestContext = Arc.container().requestContext();
        try {
            RequestContextHelper.reactivate(requestContext, dfe);
            Context vc = Vertx.currentContext();
            if (runBlocking(dfe) || BlockingHelper.blockingShouldExecuteNonBlocking(operation, vc)) {
                return super.invokeBatch(dfe, arguments);
            } else {
                return invokeBatchBlocking(dfe, arguments, vc);
            }
        } finally {
            deactivate(requestContext);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T invokeAndTransformBlocking(final io.smallrye.graphql.api.Context c, final DataFetchingEnvironment dfe,
            DataFetcherResult.Builder<Object> resultBuilder,
            Object[] transformedArguments, Context vc) throws Exception {

        SmallRyeThreadContext threadContext = Arc.container().select(SmallRyeThreadContext.class).get();
        final Promise<T> result = Promise.promise();

        // We need some make sure that we call given the context
        measurementIds.add(metricsEmitter.start(c));
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
            } catch (ConstraintViolationException cve) {
                BeanValidationUtil.addConstraintViolationsToDataFetcherResult(cve.getConstraintViolations(),
                        operationInvoker.getMethod(), resultBuilder, dfe);
                return (T) resultBuilder.build();
            } catch (Throwable ex) {
                throw ex;
            }
        });
        // Here call blocking with context
        BlockingHelper.runBlocking(vc, contextualCallable, result, operation);

        return (T) Uni.createFrom().completionStage(result.future().toCompletionStage()).onItemOrFailure()
                .invoke((item, error) -> {
                    if (item != null) {
                        eventEmitter.fireAfterDataFetch(c);
                        metricsEmitter.end(measurementIds.remove());
                    } else {
                        eventEmitter.fireOnDataFetchError(c, error);
                    }
                }).subscribeAsCompletionStage();
    }

    @SuppressWarnings("unchecked")
    private CompletionStage<List<T>> invokeBatchBlocking(DataFetchingEnvironment dfe, Object[] arguments, Context vc) {

        SmallRyeThreadContext threadContext = Arc.container().select(SmallRyeThreadContext.class).get();
        final Promise<List<T>> result = Promise.promise();

        // We need some make sure that we call given the context
        Callable<Object> contextualCallable = threadContext.contextualCallable(() -> {
            return (List<T>) operationInvoker.invokePrivileged(arguments);
        });

        // this gets called on a batch error, so that error callbacks can run with the proper context too
        Consumer<Throwable> onErrorConsumer = threadContext.contextualConsumer((Throwable exception) -> {
            io.smallrye.graphql.api.Context context = dfe.getGraphQlContext().get("context");
            eventEmitter.fireOnDataFetchError(context, exception);
        });

        // Here call blocking with context
        BlockingHelper.runBlocking(vc, contextualCallable, result, operation);
        return result.future().toCompletionStage()
                .whenComplete((resultList, error) -> {
                    if (error != null) {
                        onErrorConsumer.accept(error);
                    }
                });
    }

    private boolean runBlocking(DataFetchingEnvironment dfe) {
        return dfe.getGraphQlContext().get("runBlocking");
    }

    private void deactivate(ManagedContext requestContext) {
        SmallRyeContextManager.clearCurrentSmallRyeContext();
        requestContext.deactivate();
    }
}
