package io.quarkus.hibernate.search.orm.elasticsearch.runtime;

import java.util.Map;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.ConfigInstantiator;

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

    public HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit getPersistenceUnitConfig(String name) {
        HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit result;
        if (PersistenceUnitUtil.isDefaultPersistenceUnit(name)) {
            result = defaultPersistenceUnit;
        } else {
            result = persistenceUnits.get(name);
        }
        if (result == null) {
            result = ConfigInstantiator.createEmptyObject(HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit.class);
        }
        return result;
    }

}
