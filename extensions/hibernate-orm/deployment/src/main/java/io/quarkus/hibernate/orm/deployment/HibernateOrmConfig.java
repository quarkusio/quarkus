package io.quarkus.hibernate.orm.deployment;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.config.DatabaseOrmCompatibilityVersion;
import io.quarkus.hibernate.orm.runtime.customized.BuiltinFormatMapperBehaviour;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigMapping(prefix = "quarkus.hibernate-orm")
@ConfigRoot
public interface HibernateOrmConfig {

    /**
     * Whether Hibernate ORM is enabled *during the build*.
     *
     * If Hibernate ORM is disabled during the build, all processing related to Hibernate ORM will be skipped,
     * but it will not be possible to activate Hibernate ORM at runtime:
     * `quarkus.hibernate-orm.active` will default to `false` and setting it to `true` will lead to an error.
     *
     * @asciidoclet
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Whether Hibernate ORM is working in blocking mode.
     *
     * Hibernate ORM's blocking `EntityManager`/`Session`/`SessionFactory`
     * are normally disabled by default if no JDBC datasource is found.
     * You can set this property to `false` if you want to disable them
     * despite having a JDBC datasource.
     *
     * @asciidoclet
     */
    @WithDefault("true")
    boolean blocking();

    /**
     * Database related configuration.
     */
    @ConfigDocSection
    HibernateOrmConfigDatabase database();

    /**
     * JSON/XML mapping related configuration.
     */
    @ConfigDocSection
    HibernateOrmConfigMapping mapping();

    /**
     * Configuration for persistence units.
     */
    @WithParentName
    @WithUnnamedKey(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME)
    @WithDefaults
    @ConfigDocMapKey("persistence-unit-name")
    Map<String, HibernateOrmConfigPersistenceUnit> persistenceUnits();

    default HibernateOrmConfigPersistenceUnit defaultPersistenceUnit() {
        return persistenceUnits().get(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME);
    }

    default Map<String, HibernateOrmConfigPersistenceUnit> namedPersistenceUnits() {
        Map<String, HibernateOrmConfigPersistenceUnit> map = new TreeMap<>();
        map.putAll(persistenceUnits());
        map.remove(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME);
        return map;
    }

    /**
     * Configuration for the {@code persistence.xml} handling.
     */
    HibernateOrmConfigPersistenceXml persistenceXml();

    /**
     * Logging configuration.
     */
    @ConfigDocSection
    HibernateOrmConfigLog log();

    /**
     * Whether statistics collection is enabled. If 'metrics.enabled' is true, then the default here is
     * considered true, otherwise the default is false.
     */
    Optional<Boolean> statistics();

    /**
     * Whether session metrics should be appended into the server log for each Hibernate session. This
     * only has effect if statistics are enabled (`quarkus.hibernate-orm.statistics`). The default is false
     * (which means both `statistics` and `log-session-metrics` need to be enabled for the session metrics
     * to appear in the log).
     */
    Optional<Boolean> logSessionMetrics();

    /**
     * Configuration related to metrics.
     */
    HibernateOrmConfigMetric metrics();

    /**
     * Dev UI.
     */
    @WithDefaults
    @WithName("dev-ui")
    HibernateOrmConfigDevUI devui();

    default boolean isAnyNonPersistenceXmlPropertySet() {
        // Do NOT include persistenceXml in here.
        return defaultPersistenceUnit().isAnyPropertySet() ||
                !namedPersistenceUnits().isEmpty() ||
                log().isAnyPropertySet() ||
                statistics().isPresent() ||
                logSessionMetrics().isPresent() ||
                metrics().isAnyPropertySet();
    }

    @ConfigGroup
    interface HibernateOrmConfigPersistenceXml {

        /**
         * If {@code true}, Quarkus will ignore any {@code persistence.xml} file in the classpath
         * and rely exclusively on the Quarkus configuration.
         */
        @WithDefault("false")
        boolean ignore();

    }

    @ConfigGroup
    interface HibernateOrmConfigLog {

        /**
         * Logs SQL bind parameter.
         * <p>
         * Setting it to true is obviously not recommended in production.
         */
        @Deprecated
        @WithDefault("false")
        boolean bindParam();

        /**
         * Logs SQL bind parameters.
         * <p>
         * Setting it to true is obviously not recommended in production.
         */
        @WithDefault("false")
        boolean bindParameters();

        default boolean isAnyPropertySet() {
            return bindParam() || bindParameters();
        }
    }

    @ConfigGroup
    interface HibernateOrmConfigDatabase {
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
        @WithName("orm-compatibility.version")
        @WithDefault("latest")
        @WithConverter(DatabaseOrmCompatibilityVersion.Converter.class)
        DatabaseOrmCompatibilityVersion ormCompatibilityVersion();
    }

    @ConfigGroup
    interface HibernateOrmConfigMetric {

        /**
         * Whether metrics are published if a metrics extension is enabled.
         */
        @WithDefault("false")
        boolean enabled();

        default boolean isAnyPropertySet() {
            return enabled();
        }
    }

    @ConfigGroup
    interface HibernateOrmConfigDevUI {
        /**
         * Allow hql queries in the Dev UI page
         */
        @WithDefault("false")
        boolean allowHql();
    }

    @ConfigGroup
    interface HibernateOrmConfigMapping {

        /**
         * Mapping format.
         */
        HibernateOrmConfigMappingFormat format();

        @ConfigGroup
        interface HibernateOrmConfigMappingFormat {
            /**
             * How the default JSON/XML format mappers are configured.
             *
             * Only available to mitigate migration from the current Quarkus-preconfigured format mappers (that will be removed
             * in the future version).
             */
            @WithDefault("warn")
            BuiltinFormatMapperBehaviour global();
        }
    }
}
