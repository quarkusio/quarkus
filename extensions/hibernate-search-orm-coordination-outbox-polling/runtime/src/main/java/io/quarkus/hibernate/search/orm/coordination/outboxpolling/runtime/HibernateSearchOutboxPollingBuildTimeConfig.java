package io.quarkus.hibernate.search.orm.coordination.outboxpolling.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.hibernate-search-orm")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface HibernateSearchOutboxPollingBuildTimeConfig {

    /**
     * Configuration for the default persistence unit.
     */
    @WithParentName
    HibernateSearchOutboxPollingBuildTimeConfigPersistenceUnit defaultPersistenceUnit();

    /**
     * Configuration for additional named persistence units.
     */
    @ConfigDocSection
    @ConfigDocMapKey("persistence-unit-name")
    @WithParentName
    Map<String, HibernateSearchOutboxPollingBuildTimeConfigPersistenceUnit> persistenceUnits();

}
