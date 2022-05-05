package io.quarkus.smallrye.graphql.runtime.spi.datafetcher;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.graphql.GraphQLException;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.smallrye.graphql.SmallRyeGraphQLServerMessages;
import io.smallrye.graphql.execution.datafetcher.AbstractDataFetcher;
import io.smallrye.graphql.schema.model.Operation;
import io.smallrye.graphql.schema.model.Type;
import io.smallrye.graphql.transformation.AbstractDataFetcherException;
import io.smallrye.mutiny.Uni;

public abstract class AbstractAsyncDataFetcher<K, T> extends AbstractDataFetcher<K, T> {

    public AbstractAsyncDataFetcher(Operation operation, Type type) {
        super(operation, type);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <O> O invokeAndTransform(
            DataFetchingEnvironment dfe,
            DataFetcherResult.Builder<Object> resultBuilder,
            Object[] transformedArguments) throws Exception {

        Uni<?> uni = handleUserMethodCall(dfe, transformedArguments);
        return (O) uni
                .onItemOrFailure()
                .transformToUni((result, throwable, emitter) -> {
                    if (throwable != null) {
                        eventEmitter.fireOnDataFetchError(dfe.getExecutionId().toString(), throwable);
                        if (throwable instanceof GraphQLException) {
                            GraphQLException graphQLException = (GraphQLException) throwable;
                            errorResultHelper.appendPartialResult(resultBuilder, dfe, graphQLException);
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
                        }
                    }

                    emitter.complete(resultBuilder.build());
                })
                .subscribe()
                .asCompletionStage();
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
        try {
            return handleUserBatchLoad(dfe, arguments)
                    .subscribe().asCompletionStage();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected abstract Uni<List<T>> handleUserBatchLoad(DataFetchingEnvironment dfe, final Object[] arguments)
            throws Exception;
}
