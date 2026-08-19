package io.quarkus.datasource.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.component.AvailabilityRule;
import io.quarkus.runtime.util.ProgrammingParadigm;

/**
 * Declares an extension can handle {@link DataSourceRequestBuildItem},
 * and provides in particular an {@link AvailabilityRule} to check for unavailable datasources,
 * so that other extensions can check what can be requested.
 * <p>
 * Should not be consumed except by the "common" datasource extension;
 * other extensions should consume {@link DataSourceLookupBuildItem}.
 */
public final class DataSourceRequestHandlerBuildItem extends MultiBuildItem {
    private final ProgrammingParadigm paradigm;
    private final AvailabilityRule rule;

    public DataSourceRequestHandlerBuildItem(ProgrammingParadigm paradigm, AvailabilityRule rule) {
        this.paradigm = paradigm;
        this.rule = rule;
    }

    public ProgrammingParadigm getParadigm() {
        return paradigm;
    }

    public AvailabilityRule getAvailabilityRule() {
        return rule;
    }
}
