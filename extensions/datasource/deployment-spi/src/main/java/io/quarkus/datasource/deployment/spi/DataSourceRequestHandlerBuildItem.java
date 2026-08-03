package io.quarkus.datasource.deployment.spi;

import java.util.List;
import java.util.function.Function;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;

/**
 * Declares an extension can handle {@link DataSourceRequestBuildItem},
 * and provides in particular a way to check for unavailable datasources,
 * so that other extensions can check what can be requested.
 * <p>
 * Should not be consumed except by the "common" datasource extension;
 * other extensions should consume {@link DataSourceLookupBuildItem}.
 */
public final class DataSourceRequestHandlerBuildItem extends MultiBuildItem {
    private final ProgrammingParadigm paradigm;
    private final Function<String, List<Reason>> unavailableFunction;

    public DataSourceRequestHandlerBuildItem(ProgrammingParadigm paradigm,
            Function<String, List<Reason>> unavailableFunction) {
        this.paradigm = paradigm;
        this.unavailableFunction = unavailableFunction;
    }

    public ProgrammingParadigm getParadigm() {
        return paradigm;
    }

    public Function<String, List<Reason>> getUnavailableFunction() {
        return unavailableFunction;
    }
}
