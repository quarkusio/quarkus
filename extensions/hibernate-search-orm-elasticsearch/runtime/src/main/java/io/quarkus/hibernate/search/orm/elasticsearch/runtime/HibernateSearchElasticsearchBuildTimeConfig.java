package io.quarkus.hibernate.search.orm.elasticsearch.runtime;

import java.util.Map;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

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
     * Configuration for persistence units.
     */
    @WithParentName
    @WithUnnamedKey(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME)
    @ConfigDocMapKey("persistence-unit-name")
    Map<String, HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit> persistenceUnits();

    /**
     * Management interface.
     */
    @ConfigDocSection
    HibernateSearchElasticsearchBuildTimeConfigManagement management();
}
