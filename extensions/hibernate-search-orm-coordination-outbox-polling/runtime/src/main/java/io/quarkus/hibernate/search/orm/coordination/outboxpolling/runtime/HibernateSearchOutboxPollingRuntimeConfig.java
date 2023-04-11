package io.quarkus.hibernate.search.orm.coordination.outboxpolling.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.hibernate-search-orm")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface HibernateSearchOutboxPollingRuntimeConfig {

    /**
     * Configuration for the default persistence unit.
     */
    @WithParentName
    HibernateSearchOutboxPollingRuntimeConfigPersistenceUnit defaultPersistenceUnit();

    /**
     * Configuration for additional named persistence units.
     */
    @ConfigDocSection
    @ConfigDocMapKey("persistence-unit-name")
    @WithParentName
    Map<String, HibernateSearchOutboxPollingRuntimeConfigPersistenceUnit> persistenceUnits();

}
