package io.quarkus.hibernate.orm.deployment.component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfigPersistenceUnit;
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
    private final Optional<AdditionalConfig> additionalConfig;

    public PersistenceUnitDefinitionBuildItem(String persistenceUnitName, ProgrammingParadigm paradigm,
            List<Reason> reasons,
            HibernateOrmConfigPersistenceUnit config, Optional<String> dataSourceName,
            Optional<AdditionalConfig> additionalConfig) {
        this.persistenceUnitName = persistenceUnitName;
        this.paradigm = paradigm;
        this.reasons = reasons;
        this.config = config;
        this.dataSourceName = dataSourceName;
        this.additionalConfig = additionalConfig;
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

    /**
     * @return additional configuration from
     *         {@link io.quarkus.hibernate.orm.deployment.spi.AdditionalPersistenceUnitBuildItem},
     *         or empty for PUs defined through Quarkus configuration.
     */
    public Optional<AdditionalConfig> getAdditionalConfig() {
        return additionalConfig;
    }

    /**
     * Configuration contributed by an extension through
     * {@link io.quarkus.hibernate.orm.deployment.spi.AdditionalPersistenceUnitBuildItem}.
     */
    public record AdditionalConfig(
            Optional<String> dataSourceName,
            Optional<String> explicitDialect,
            Map<String, String> properties) {
    }
}
