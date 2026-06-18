package io.quarkus.datasource.deployment.spi;

import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.util.ProgrammingParadigm;

/**
 * Represents a datasource that has been determined to exist.
 * <p>
 * Each datasource appears at most once, with the set of paradigms it was defined for.
 * Produced from {@link DataSourceDefinitionBuildItem} instances.
 */
public final class DataSourceDefinedBuildItem extends MultiBuildItem {

    private final String name;
    private final String dbKind;
    private final Set<ProgrammingParadigm> paradigms;

    public DataSourceDefinedBuildItem(String name, String dbKind, Set<ProgrammingParadigm> paradigms) {
        this.name = name;
        this.dbKind = dbKind;
        this.paradigms = paradigms;
    }

    public String getName() {
        return name;
    }

    public String getDbKind() {
        return dbKind;
    }

    public Set<ProgrammingParadigm> getParadigms() {
        return paradigms;
    }
}
