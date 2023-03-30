package io.quarkus.hibernate.search.orm.elasticsearch.runtime;

import java.util.Map;
import java.util.TreeMap;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.hibernate-search-orm")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface HibernateSearchElasticsearchBuildTimeConfig {

    /**
     * Whether Hibernate Search is enabled **during the build**.
     *
     * If Hibernate Search is disabled during the build, all processing related to Hibernate Search will be skipped,
     * but it will not be possible to activate Hibernate Search at runtime:
     * `quarkus.hibernate-search-orm.active` will default to `false` and setting it to `true` will lead to an error.
     *
     * @asciidoclet
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Configuration for the default persistence unit.
     */
    @WithParentName
    HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit defaultPersistenceUnit();

    /**
     * Configuration for additional named persistence units.
     */
    @ConfigDocSection
    @ConfigDocMapKey("persistence-unit-name")
    @WithParentName
    Map<String, HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit> persistenceUnits();

    default Map<String, HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit> getAllPersistenceUnitConfigsAsMap() {
        Map<String, HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit> map = new TreeMap<>();
        HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit defaultPersistenceUnit = defaultPersistenceUnit();

        if (defaultPersistenceUnit != null) {
            map.put(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME, defaultPersistenceUnit);
        }
        map.putAll(persistenceUnits());
        return map;
    }

    default HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit getPersistenceUnitConfig(String persistenceUnitName) {
        if (persistenceUnitName == null) {
            throw new IllegalArgumentException("Persistence unit name may not be null");
        }

        if (PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME.equals(persistenceUnitName)) {
            return defaultPersistenceUnit();
        }

        HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit persistenceUnitConfig = persistenceUnits()
                .get(persistenceUnitName);

        if (persistenceUnitConfig == null) {
            throw new IllegalStateException("No persistence unit config exists for name " + persistenceUnitName);
        }

        return persistenceUnitConfig;
    }
}
