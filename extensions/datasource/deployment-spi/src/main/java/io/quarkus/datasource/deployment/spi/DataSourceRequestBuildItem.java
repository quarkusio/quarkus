package io.quarkus.datasource.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;

/**
 * Represents a request for a datasource to be created.
 * <p>
 * Extensions can produce this build item to indicate they need a specific datasource,
 * even if that datasource is not explicitly configured. The datasource extension will
 * then ensure the datasource exists or produce a helpful error message if it cannot be created.
 * <p>
 * Acknowledged requests will yield a {@link DataSourceDefinedBuildItem} later on.
 */
public final class DataSourceRequestBuildItem extends MultiBuildItem {

    private final String name;
    private final ProgrammingParadigm paradigm;
    private final Reason reason;

    public DataSourceRequestBuildItem(String name, ProgrammingParadigm paradigm, String reason) {
        this(name, paradigm, new Reason(reason));
    }

    public DataSourceRequestBuildItem(String name, ProgrammingParadigm paradigm, Reason reason) {
        this.name = name;
        this.paradigm = paradigm;
        this.reason = reason;
    }

    public String getName() {
        return name;
    }

    public ProgrammingParadigm getParadigm() {
        return paradigm;
    }

    public Reason getReason() {
        return reason;
    }
}
