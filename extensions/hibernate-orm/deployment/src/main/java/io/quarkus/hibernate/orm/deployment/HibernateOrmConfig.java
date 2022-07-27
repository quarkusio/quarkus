package io.quarkus.hibernate.orm.deployment;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class HibernateOrmConfig {

    /**
     * Whether Hibernate ORM is enabled <strong>during the build</strong>.
     *
     * If Hibernate ORM is disabled during the build, all processing related to Hibernate ORM will be skipped,
     * but it will not be possible to use Hibernate ORM at runtime.
     *
     * @asciidoclet
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

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
     * Whether session metrics should be appended into the server log for each Hibernate session. This
     * only has effect if statistics are enabled (`quarkus.hibernate-orm.statistics`). The default is false
     * (which means both `statistics` and `log-session-metrics` need to be enabled for the session metrics
     * to appear in the log).
     */
    @ConfigItem
    public Optional<Boolean> logSessionMetrics;

    /**
     * Whether metrics are published if a metrics extension is enabled.
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

    public Map<String, HibernateOrmConfigPersistenceUnit> getAllPersistenceUnitConfigsAsMap() {
        Map<String, HibernateOrmConfigPersistenceUnit> map = new TreeMap<>();
        if (defaultPersistenceUnit != null) {
            map.put(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME, defaultPersistenceUnit);
        }
        map.putAll(persistenceUnits);
        return map;
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
