package io.quarkus.hibernate.search.orm.coordination.outboxpolling.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "hibernate-search-orm", phase = ConfigPhase.RUN_TIME)
public class HibernateSearchOutboxPollingRuntimeConfig {

    /**
     * Configuration for the default persistence unit.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public HibernateSearchOutboxPollingRuntimeConfigPersistenceUnit defaultPersistenceUnit;

    /**
     * Configuration for additional named persistence units.
     */
    @ConfigDocSection
    @ConfigDocMapKey("persistence-unit-name")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, HibernateSearchOutboxPollingRuntimeConfigPersistenceUnit> persistenceUnits;

}
