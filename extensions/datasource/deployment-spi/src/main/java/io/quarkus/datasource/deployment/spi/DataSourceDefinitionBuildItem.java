package io.quarkus.datasource.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.util.ProgrammingParadigm;

/**
 * Declares that a datasource has been defined for a given programming paradigm.
 * <p>
 * Produced by Agroal (for JDBC/BLOCKING) and Reactive datasource processors (for REACTIVE).
 * Consumed by {@link io.quarkus.datasource.deployment.spi.DataSourceDefinedBuildItem DataSourceDefinedBuildItem},
 * which aggregates these into a deduplicated view with all paradigms per datasource.
 */
public final class DataSourceDefinitionBuildItem extends MultiBuildItem {

    private final String name;
    private final String dbKind;
    private final ProgrammingParadigm paradigm;

    public DataSourceDefinitionBuildItem(String name, String dbKind, ProgrammingParadigm paradigm) {
        this.name = name;
        this.dbKind = dbKind;
        this.paradigm = paradigm;
    }

    public String getName() {
        return name;
    }

    public String getDbKind() {
        return dbKind;
    }

    public ProgrammingParadigm getParadigm() {
        return paradigm;
    }
}
