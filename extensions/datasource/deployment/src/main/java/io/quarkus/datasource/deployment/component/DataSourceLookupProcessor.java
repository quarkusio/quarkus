package io.quarkus.datasource.deployment.component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import io.quarkus.datasource.deployment.spi.component.DataSourceLookupBuildItem;
import io.quarkus.datasource.deployment.spi.component.DataSourceRequestHandlerBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.component.ComponentLookup;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;

/**
 * Produces a {@link DataSourceLookupBuildItem datasource lookup}
 * by assembling a {@link io.quarkus.deployment.component.ComponentLookup} from
 * {@link DataSourceRequestHandlerBuildItem} contributions provided by extension-specific
 * processors (Agroal for JDBC, reactive-datasource for reactive).
 *
 * @see io.quarkus.agroal.deployment.component.DataSourceDefinitionBlockingProcessor
 * @see io.quarkus.reactive.datasource.deployment.component.DataSourceDefinitionReactiveProcessor
 */
class DataSourceLookupProcessor {

    @BuildStep
    DataSourceLookupBuildItem defineLookup(List<DataSourceRequestHandlerBuildItem> handlers) {
        List<ComponentLookup> delegates = new ArrayList<>();
        EnumSet<ProgrammingParadigm> unhandledParadigms = EnumSet.allOf(ProgrammingParadigm.class);
        for (DataSourceRequestHandlerBuildItem handler : handlers) {
            var paradigm = handler.getParadigm();
            if (!unhandledParadigms.remove(paradigm)) {
                throw new IllegalStateException("Multiple " + paradigm + " datasource request handlers: " + handlers);
            }
            delegates.add(handler.getLookup());
        }
        for (ProgrammingParadigm unhandled : unhandledParadigms) {
            delegates.add(missingExtensionLookup(unhandled));
        }
        return new DataSourceLookupBuildItem(ComponentLookup.of(delegates));
    }

    private static ComponentLookup missingExtensionLookup(ProgrammingParadigm missing) {
        String message = switch (missing) {
            case BLOCKING -> "Agroal extension is absent";
            case REACTIVE -> "Reactive Datasource extension is absent";
        };
        return (name, paradigm) -> paradigm == missing ? List.of(new Reason(message)) : List.of();
    }
}
