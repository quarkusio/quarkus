package io.quarkus.hibernate.orm.deployment;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.config.DatabaseOrmCompatibilityVersion;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.ConvertWith;

@ConfigRoot
public class HibernateOrmConfig {

    /**
     * Whether Hibernate ORM is enabled *during the build*.
     *
     * If Hibernate ORM is disabled during the build, all processing related to Hibernate ORM will be skipped,
     * but it will not be possible to activate Hibernate ORM at runtime:
     * `quarkus.hibernate-orm.active` will default to `false` and setting it to `true` will lead to an error.
     *
     * @asciidoclet
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * Database related configuration.
     */
    @ConfigItem
    @ConfigDocSection
    public HibernateOrmConfigDatabase database;

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
     * Configuration for the {@code persistence.xml} handling.
     */
    @ConfigItem
    public HibernateOrmConfigPersistenceXml persistenceXml;

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

    public boolean isAnyNonPersistenceXmlPropertySet() {
        // Do NOT include persistenceXml in here.
        return defaultPersistenceUnit.isAnyPropertySet() ||
                !persistenceUnits.isEmpty() ||
                log.isAnyPropertySet() ||
                statistics.isPresent() ||
                logSessionMetrics.isPresent() ||
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
    public static class HibernateOrmConfigPersistenceXml {

        /**
         * If {@code true}, Quarkus will ignore any {@code persistence.xml} file in the classpath
         * and rely exclusively on the Quarkus configuration.
         */
        @ConfigItem
        public boolean ignore;

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

    @ConfigGroup
    public static class HibernateOrmConfigDatabase {
        /**
         * When set, attempts to exchange data with the database
         * as the given version of Hibernate ORM would have,
         * *on a best-effort basis*.
         *
         * Please note:
         *
         * * schema validation may still fail in some cases:
         * this attempts to make Hibernate ORM 6+ behave correctly at runtime,
         * but it may still expect a different (but runtime-compatible) schema.
         * * robust test suites are still useful and recommended:
         * you should still check that your application behaves as intended with your legacy schema.
         * * this feature is inherently unstable:
         * some aspects of it may stop working in future versions of Quarkus,
         * and older versions will be dropped as Hibernate ORM changes pile up
         * and support for those older versions becomes too unreliable.
         * * you should still plan a migration of your schema to a newer version of Hibernate ORM.
         * For help with migration, refer to
         * link:https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.0:-Hibernate-ORM-5-to-6-migration[the Quarkus 3
         * migration guide from Hibernate ORM 5 to 6].
         *
         * @asciidoclet
         */
        @ConfigItem(name = "orm-compatibility.version", defaultValue = "latest")
        @ConvertWith(DatabaseOrmCompatibilityVersion.Converter.class)
        public DatabaseOrmCompatibilityVersion ormCompatibilityVersion;
    }

}
