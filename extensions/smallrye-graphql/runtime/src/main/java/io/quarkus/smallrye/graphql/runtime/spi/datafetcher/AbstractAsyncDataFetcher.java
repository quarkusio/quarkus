package io.quarkus.smallrye.graphql.runtime.spi.datafetcher;

import java.util.List;
import java.util.concurrent.CompletionStage;

import jakarta.validation.ConstraintViolationException;

import org.eclipse.microprofile.graphql.GraphQLException;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.smallrye.graphql.SmallRyeGraphQLServerMessages;
import io.smallrye.graphql.api.Context;
import io.smallrye.graphql.execution.context.SmallRyeContextManager;
import io.smallrye.graphql.execution.datafetcher.AbstractDataFetcher;
import io.smallrye.graphql.schema.model.Operation;
import io.smallrye.graphql.schema.model.Type;
import io.smallrye.graphql.transformation.AbstractDataFetcherException;
import io.smallrye.graphql.validation.BeanValidationUtil;
import io.smallrye.mutiny.Uni;

public abstract class AbstractAsyncDataFetcher<K, T> extends AbstractDataFetcher<K, T> {

    public AbstractAsyncDataFetcher(Operation operation, Type type) {
        super(operation, type);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <O> O invokeAndTransform(
            Context c,
            DataFetchingEnvironment dfe,
            DataFetcherResult.Builder<Object> resultBuilder,
            Object[] transformedArguments) throws Exception {
        ManagedContext requestContext = Arc.container().requestContext();

        try {
            measurementIds.add(metricsEmitter.start(c));
            RequestContextHelper.reactivate(requestContext, dfe);
            Uni<?> uni = handleUserMethodCall(dfe, transformedArguments);
            return (O) uni
                    .onItemOrFailure()
                    .transformToUni((result, throwable, emitter) -> {

                        try {
                            emitter.onTermination(() -> {
                                deactivate(requestContext);
                            });
                            if (throwable != null) {
                                eventEmitter.fireOnDataFetchError(c, throwable);
                                if (throwable instanceof GraphQLException) {
                                    GraphQLException graphQLException = (GraphQLException) throwable;
                                    errorResultHelper.appendPartialResult(resultBuilder, dfe, graphQLException);
                                } else if (throwable instanceof ConstraintViolationException) {
                                    BeanValidationUtil.addConstraintViolationsToDataFetcherResult(
                                            ((ConstraintViolationException) throwable).getConstraintViolations(),
                                            operationInvoker.getMethod(), resultBuilder, dfe);
                                } else if (throwable instanceof Exception) {
                                    emitter.fail(SmallRyeGraphQLServerMessages.msg.dataFetcherException(operation, throwable));
                                    return;
                                } else if (throwable instanceof Error) {
                                    emitter.fail(throwable);
                                    return;
                                }
                            } else {
                                try {
                                    resultBuilder.data(fieldHelper.transformOrAdaptResponse(result, dfe));
                                } catch (AbstractDataFetcherException te) {
                                    te.appendDataFetcherResult(resultBuilder, dfe);
                                } finally {
                                    eventEmitter.fireAfterDataFetch(c);
                                }
                            }
                            emitter.complete(resultBuilder.build());
                        } finally {
                            metricsEmitter.end(measurementIds.remove());
                        }
                    })
                    .onCancellation().invoke(() -> {
                        deactivate(requestContext);
                    })
                    .onTermination().invoke(() -> {
                        deactivate(requestContext);
                    })
                    .subscribe()
                    .asCompletionStage();
        } finally {
            deactivate(requestContext);
        }
    }

    private void deactivate(ManagedContext requestContext) {
        SmallRyeContextManager.clearCurrentSmallRyeContext();
        requestContext.deactivate();
    }

    protected abstract Uni<?> handleUserMethodCall(DataFetchingEnvironment dfe, final Object[] transformedArguments)
            throws Exception;

    @Override
    @SuppressWarnings("unchecked")
    protected <O> O invokeFailure(DataFetcherResult.Builder<Object> resultBuilder) {
        return (O) Uni.createFrom()
                .item(resultBuilder::build)
                .subscribe()
                .asCompletionStage();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected CompletionStage<List<T>> invokeBatch(DataFetchingEnvironment dfe, Object[] arguments) {
        ManagedContext requestContext = Arc.container().requestContext();
        try {
            RequestContextHelper.reactivate(requestContext, dfe);
            return handleUserBatchLoad(dfe, arguments)
                    .subscribe().asCompletionStage();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            requestContext.deactivate();
        }
    }

    protected abstract Uni<List<T>> handleUserBatchLoad(DataFetchingEnvironment dfe, final Object[] arguments)
            throws Exception;
}
