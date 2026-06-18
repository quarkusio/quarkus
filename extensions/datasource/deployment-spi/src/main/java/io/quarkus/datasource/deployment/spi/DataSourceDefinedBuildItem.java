package io.quarkus.datasource.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.util.ProgrammingParadigm;

/**
 * Represents a datasource that has been determined to exist, either because it was
 * explicitly configured or because it was referenced by another extension.
 * <p>
 * This is the authoritative source of information for the set of datasources that will be created.
 * <p>
 * Produced by Agroal (for JDBC) and Reactive datasource processors (for reactive).
 * Consumed by dev services and other processors that need to know which datasources exist.
 */
public final class DataSourceDefinedBuildItem extends MultiBuildItem {

    private final String name;
    private final ProgrammingParadigm paradigm;
    private final String dbKind;

    public DataSourceDefinedBuildItem(String name, ProgrammingParadigm paradigm, String dbKind) {
        this.name = name;
        this.paradigm = paradigm;
        this.dbKind = dbKind;
    }

    public String getName() {
        return name;
    }

    public ProgrammingParadigm getParadigm() {
        return paradigm;
    }

    public String getDbKind() {
        return dbKind;
    }

}
