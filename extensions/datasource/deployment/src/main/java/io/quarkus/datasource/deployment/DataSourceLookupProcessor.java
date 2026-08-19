package io.quarkus.datasource.deployment;

import java.util.List;
import java.util.function.Function;

import io.quarkus.datasource.deployment.spi.DataSourceLookupBuildItem;
import io.quarkus.datasource.deployment.spi.DataSourceRequestHandlerBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.component.ComponentLookup;
import io.quarkus.runtime.util.Reason;

/**
 * Produces a {@link DataSourceLookupBuildItem datasource lookup}
 * by assembling a {@link io.quarkus.deployment.component.ComponentLookup} from
 * {@link DataSourceRequestHandlerBuildItem} contributions provided by extension-specific
 * processors (Agroal for JDBC, reactive-datasource for reactive).
 *
 * @see io.quarkus.agroal.deployment.DataSourceDefinitionBlockingProcessor
 * @see io.quarkus.reactive.datasource.deployment.DataSourceDefinitionReactiveProcessor
 */
class DataSourceLookupProcessor {

    @BuildStep
    DataSourceLookupBuildItem defineLookup(List<DataSourceRequestHandlerBuildItem> handlers) {
        boolean blockingFound = false;
        boolean reactiveFound = false;
        Function<String, List<Reason>> blockingUnavailableFunction = ignored -> List
                .of(new Reason("Agroal extension is absent"));
        Function<String, List<Reason>> reactiveUnavailableFunction = ignored -> List
                .of(new Reason("Reactive Datasource extension is absent"));
        for (DataSourceRequestHandlerBuildItem handler : handlers) {
            switch (handler.getParadigm()) {
                case BLOCKING -> {
                    if (blockingFound) {
                        throw new IllegalStateException("Multiple blocking datasource request handlers " + handlers);
                    }
                    blockingFound = true;
                    blockingUnavailableFunction = handler.getUnavailableFunction();
                }
                case REACTIVE -> {
                    if (reactiveFound) {
                        throw new IllegalStateException("Multiple blocking datasource request handlers " + handlers);
                    }
                    reactiveFound = true;
                    reactiveUnavailableFunction = handler.getUnavailableFunction();
                }
            }
        }

        return new DataSourceLookupBuildItem(
                ComponentLookup.of(blockingUnavailableFunction, reactiveUnavailableFunction));
    }
}
