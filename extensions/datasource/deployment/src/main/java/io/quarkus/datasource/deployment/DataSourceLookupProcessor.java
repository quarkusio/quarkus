package io.quarkus.datasource.deployment;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.datasource.deployment.spi.DataSourceLookupBuildItem;
import io.quarkus.datasource.deployment.spi.DataSourceRequestHandlerBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.component.AvailabilityRule;
import io.quarkus.deployment.component.ComponentLookup;
import io.quarkus.runtime.util.ProgrammingParadigm;
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
        List<AvailabilityRule> rules = new ArrayList<>();
        boolean blockingFound = false;
        boolean reactiveFound = false;
        for (DataSourceRequestHandlerBuildItem handler : handlers) {
            rules.add(handler.getAvailabilityRule());
            switch (handler.getParadigm()) {
                case BLOCKING -> blockingFound = true;
                case REACTIVE -> reactiveFound = true;
            }
        }
        if (!blockingFound) {
            rules.add((paradigm, name) -> paradigm == ProgrammingParadigm.BLOCKING
                    ? List.of(new Reason("Agroal extension is absent"))
                    : List.of());
        }
        if (!reactiveFound) {
            rules.add((paradigm, name) -> paradigm == ProgrammingParadigm.REACTIVE
                    ? List.of(new Reason("Reactive Datasource extension is absent"))
                    : List.of());
        }
        return new DataSourceLookupBuildItem(ComponentLookup.of(rules));
    }
}
