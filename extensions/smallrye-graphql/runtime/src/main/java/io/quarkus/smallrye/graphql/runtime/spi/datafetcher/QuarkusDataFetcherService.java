package io.quarkus.smallrye.graphql.runtime.spi.datafetcher;

import io.smallrye.graphql.execution.datafetcher.PlugableBatchableDataFetcher;
import io.smallrye.graphql.schema.model.Operation;
import io.smallrye.graphql.schema.model.Type;
import io.smallrye.graphql.spi.DataFetcherService;

/**
 * Some Quarkus specific datafetchers to execute reactive on the correct thread
 */
public class QuarkusDataFetcherService implements DataFetcherService {

    private final int priority = 1;

    @Override
    public Integer getPriority() {
        return priority;
    }

    @Override
    public PlugableBatchableDataFetcher getUniDataFetcher(Operation operation, Type type) {
        return new QuarkusUniDataFetcher(operation, type);
    }

    @Override
    public PlugableBatchableDataFetcher getDefaultDataFetcher(Operation operation, Type type) {
        return new QuarkusDefaultDataFetcher(operation, type);
    }

    @Override
    public PlugableBatchableDataFetcher getCompletionStageDataFetcher(Operation operation, Type type) {
        return new QuarkusCompletionStageDataFetcher(operation, type);
    }
}
