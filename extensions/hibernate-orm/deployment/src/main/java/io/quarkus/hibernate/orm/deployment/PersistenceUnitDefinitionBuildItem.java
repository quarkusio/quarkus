package io.quarkus.hibernate.orm.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;

/**
 * The first build item created after the decision was taken to define a persistence unit.
 * <p>
 * It holds build-time configuration and various PU-related information that is resolved early.
 */
public final class PersistenceUnitDefinitionBuildItem extends MultiBuildItem {

    private final String persistenceUnitName;
    private final ProgrammingParadigm paradigm;
    private final List<Reason> reasons;
    private final HibernateOrmConfigPersistenceUnit config;
    private final Optional<String> dataSourceName;

    public PersistenceUnitDefinitionBuildItem(String persistenceUnitName, ProgrammingParadigm paradigm,
            List<Reason> reasons,
            HibernateOrmConfigPersistenceUnit config, Optional<String> dataSourceName) {
        this.persistenceUnitName = persistenceUnitName;
        this.paradigm = paradigm;
        this.reasons = reasons;
        this.config = config;
        this.dataSourceName = dataSourceName;
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    public ProgrammingParadigm getParadigm() {
        return paradigm;
    }

    public List<Reason> getReasons() {
        return reasons;
    }

    public HibernateOrmConfigPersistenceUnit getConfig() {
        return config;
    }

    public Optional<String> getDataSourceName() {
        return dataSourceName;
    }
}
