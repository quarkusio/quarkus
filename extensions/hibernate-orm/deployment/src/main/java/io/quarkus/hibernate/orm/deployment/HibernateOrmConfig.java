package io.quarkus.hibernate.orm.deployment;

import java.util.Map;
import java.util.Optional;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class HibernateOrmConfig {

    /**
     * Configuration for the default persistence unit.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public HibernateOrmConfigPersistenceUnit defaultPersistenceUnit;

    /**
     * Additional named persistence units.
     */
    @ConfigDocSection
    @ConfigDocMapKey("persistence-unit-name")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, HibernateOrmConfigPersistenceUnit> persistenceUnits;

    /**
     * Logging configuration.
     */
    @ConfigItem
    @ConfigDocSection
    public HibernateOrmConfigLog log;

    /**
     * Whether statistics collection is enabled. If 'metrics.enabled' is true, then the default here is
     * considered true, otherwise the default is false.
     */
    @ConfigItem
    public Optional<Boolean> statistics;

    /**
     * Whether or not metrics are published if a metrics extension is enabled.
     */
    @ConfigItem(name = "metrics.enabled")
    public boolean metricsEnabled;

    public boolean isAnyPropertySet() {
        return defaultPersistenceUnit.isAnyPropertySet() ||
                !persistenceUnits.isEmpty() ||
                log.isAnyPropertySet() ||
                statistics.isPresent() ||
                metricsEnabled;
    }

    public static String puPropertyKey(String puName, String radical) {
        String prefix = PersistenceUnitUtil.isDefaultPersistenceUnit(puName)
                ? "quarkus.hibernate-orm."
                : "quarkus.hibernate-orm.\"" + puName + "\".";
        return prefix + radical;
    }

    @ConfigGroup
    public static class HibernateOrmConfigLog {

        /**
         * Logs SQL bind parameter.
         * <p>
         * Setting it to true is obviously not recommended in production.
         */
        @ConfigItem
        @Deprecated
        public boolean bindParam;

        /**
         * Logs SQL bind parameters.
         * <p>
         * Setting it to true is obviously not recommended in production.
         */
        @ConfigItem
        public boolean bindParameters;

        public boolean isAnyPropertySet() {
            return bindParam || bindParameters;
        }
    }
}
