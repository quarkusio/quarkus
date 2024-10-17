package io.quarkus.flyway.multitenant.runtime;

import java.util.Collections;
import java.util.Map;

import io.quarkus.flyway.runtime.FlywayDataSourceRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "flyway.multitenant", phase = ConfigPhase.RUN_TIME)
public final class FlywayMultiTenantRuntimeConfig {

    /**
     * Gets the {@link FlywayDataSourceRuntimeConfig} for the given datasource name.
     */
    public FlywayDataSourceRuntimeConfig getConfigForPersistenceUnitName(String persistenceUnitName) {
        if (PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName)) {
            return defaultPersistenceUnit;
        }
        return namedPersistenceUnits.getOrDefault(persistenceUnitName, FlywayDataSourceRuntimeConfig.defaultConfig());
    }

    /**
     * Flyway configuration for the default datasource.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public FlywayDataSourceRuntimeConfig defaultPersistenceUnit = FlywayDataSourceRuntimeConfig.defaultConfig();

    /**
     * Named persistenceUnits.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    @ConfigDocMapKey("datasource-name")
    @ConfigDocSection
    public Map<String, FlywayDataSourceRuntimeConfig> namedPersistenceUnits = Collections.emptyMap();
}
