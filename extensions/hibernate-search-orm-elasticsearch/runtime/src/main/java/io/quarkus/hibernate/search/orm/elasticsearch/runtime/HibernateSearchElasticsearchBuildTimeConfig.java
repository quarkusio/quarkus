package io.quarkus.hibernate.search.orm.elasticsearch.runtime;

import java.util.Map;
import java.util.TreeMap;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "hibernate-search-orm", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class HibernateSearchElasticsearchBuildTimeConfig {

    /**
     * Whether Hibernate Search is enabled <strong>during the build</strong>.
     *
     * If Hibernate Search is disabled during the build, all processing related to Hibernate Search will be skipped,
     * but it will not be possible to activate Hibernate Search at runtime:
     * `quarkus.hibernate-search-orm.active` will default to `false` and setting it to `true` will lead to an error.
     *
     * @asciidoclet
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * Configuration for the default persistence unit.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit defaultPersistenceUnit;

    /**
     * Configuration for additional named persistence units.
     */
    @ConfigDocSection
    @ConfigDocMapKey("persistence-unit-name")
    @ConfigItem(name = ConfigItem.PARENT)
    Map<String, HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit> persistenceUnits;

    public Map<String, HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit> getAllPersistenceUnitConfigsAsMap() {
        Map<String, HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit> map = new TreeMap<>();
        if (defaultPersistenceUnit != null) {
            map.put(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME, defaultPersistenceUnit);
        }
        map.putAll(persistenceUnits);
        return map;
    }

}
