package io.quarkus.hibernate.orm.deployment;

import java.util.Optional;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.util.ProgrammingParadigm;

/**
 * Represents a persistence unit that has been determined to exist.
 * <p>
 * Each persistence unit appears at most once, with the set of paradigms it was defined for.
 * Produced from {@link PersistenceUnitDefinitionBuildItem} instances.
 */
public final class PersistenceUnitDefinedBuildItem extends MultiBuildItem {

    private final String persistenceUnitName;
    private final Optional<String> dataSourceName;
    private final Set<ProgrammingParadigm> paradigms;

    public PersistenceUnitDefinedBuildItem(String persistenceUnitName,
            Optional<String> dataSourceName, Set<ProgrammingParadigm> paradigms) {
        this.persistenceUnitName = persistenceUnitName;
        this.dataSourceName = dataSourceName;
        this.paradigms = paradigms;
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    public Optional<String> getDataSourceName() {
        return dataSourceName;
    }

    public Set<ProgrammingParadigm> getParadigms() {
        return paradigms;
    }
}
