package io.quarkus.hibernate.orm.deployment;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class HibernateOrmConfig {
    /**
     * The hibernate ORM dialect class name.
     * If not present, guessed from the JDBC driver.
     *
     * The complete list of bundled dialects is available in the
     * <a target="_top" href=
     * "https://docs.jboss.org/hibernate/stable/orm/javadocs/org/hibernate/dialect/package-summary.html">Hibernate ORM
     * JavaDoc</a>.
     */
    // TODO should it be dialects
    //TODO should it be shortcuts like "postgresql" "h2" etc
    @ConfigItem
    public Optional<String> dialect;

    /**
     * The storage engine used by the dialect if it supports several storage engines
     * (e.g. MyISAM or InnoDB).
     * <p>
     * This is the case of MariaDB.
     */
    @ConfigItem(name = "dialect.storage-engine")
    public Optional<String> dialectStorageEngine;

    /**
     * (defaults to {@code /import.sql}). Name of the file containing the SQL statements to execute when Hibernate ORM starts.
     * <p>
     * By default, simply add {@code import.sql} in the root of your resources directory
     * and it will be picked up without having to set this property.
     * Pass {@code no-file} to force Hibernate ORM to ignore the SQL import file.
     * If you need different SQL statements between dev mode, test ({@code @QuarkusTest}) and in production,
     * use Quarkus
     * <a target="_top" href="https://quarkus.io/guides/application-configuration-guide#configuration-profiles">configuration
     * profiles facility</a>.
     *
     * <pre>
     * {@code
     * # application.properties
     * %dev.quarkus.hibernate-orm.sql-load-script = import-dev.sql
     * %test.quarkus.hibernate-orm.sql-load-script = import-test.sql
     * %prod.quarkus.hibernate-orm.sql-load-script = no-file
     * }
     * </pre>
     * <p>
     * Quarkus supports {@code .sql} file with SQL statements or comments spread over multiple lines.
     * Each SQL statement must be terminated by a semicolon.
     */
    @ConfigItem
    public Optional<String> sqlLoadScript;

    /**
     * The size of a batch when using batch loading to load entities and collections.
     * <p>
     * {@code -1} means batch loading is disabled.
     */
    @ConfigItem(defaultValue = "-1")
    public int batchFetchSize;

    /**
     * Query related configuration.
     */
    @ConfigItem
    public HibernateOrmConfigQuery query;

    /**
     * Database related configuration.
     */
    @ConfigItem
    public HibernateOrmConfigDatabase database;

    /**
     * JDBC related configuration.
     */
    @ConfigItem
    public HibernateOrmConfigJdbc jdbc;

    /**
     * Logging configuration.
     */
    @ConfigItem
    public HibernateOrmConfigLog log;

    /**
     * Caching configuration
     */
    public Map<String, HibernateOrmConfigCache> cache;

    /**
     * (defaults to {@code false}). Whether statistics collection is enabled.
     */
    @ConfigItem(defaultValue = "false")
    public boolean statistics;

    public boolean isAnyPropertySet() {
        return dialect.isPresent() ||
                dialectStorageEngine.isPresent() ||
                sqlLoadScript.isPresent() ||
                batchFetchSize > 0 ||
                statistics ||
                query.isAnyPropertySet() ||
                database.isAnyPropertySet() ||
                jdbc.isAnyPropertySet() ||
                log.isAnyPropertySet() ||
                !cache.isEmpty();
    }

    @ConfigGroup
    public static class HibernateOrmConfigQuery {

        /**
         * The maximum size of the query plan cache.
         */
        @ConfigItem
        public Optional<String> queryPlanCacheMaxSize;

        /**
         * Defaults to {@code none}.
         * The default ordering of nulls specific in the ORDER BY clause.
         * <p>
         * Valid values are: {@code none, first, last}.
         */
        @ConfigItem
        public Optional<String> defaultNullOrdering;

        public boolean isAnyPropertySet() {
            return queryPlanCacheMaxSize.isPresent() || defaultNullOrdering.isPresent();
        }
    }

    @ConfigGroup
    public static class HibernateOrmConfigDatabase {

        /**
         * Select whether the database schema is generated or not.
         * Options are {@code none, create, drop-and-create, drop, update}.
         * The default is {@code none}.
         * {@code drop-and-create} which is awesome in development mode.
         * <p>
         * Same as JPA's javax.persistence.schema-generation.database.action.
         */
        @ConfigItem
        public Optional<String> generation;

        /**
         * Whether we should stop schema application at the first error or continue.
         */
        @ConfigItem(name = "generation.halt-on-error", defaultValue = "false")
        public boolean generationHaltOnError;

        /**
         * The default database catalog.
         */
        @ConfigItem
        public Optional<String> defaultCatalog;

        /**
         * The default database schema.
         */
        @ConfigItem
        public Optional<String> defaultSchema;

        /**
         * The charset of the database.
         */
        @ConfigItem
        public Optional<String> charset;

        public boolean isAnyPropertySet() {
            return generation.isPresent() || defaultCatalog.isPresent() || defaultSchema.isPresent() || generationHaltOnError
                    || charset.isPresent();
        }
    }

    @ConfigGroup
    public static class HibernateOrmConfigJdbc {

        /**
         * The timezone pushed to the JDBC driver.
         */
        @ConfigItem
        public Optional<String> timezone;

        /**
         * How many rows are fetched at a time by the JDBC driver.
         */
        @ConfigItem
        public Optional<Integer> statementFetchSize;

        /**
         * The number of updates (inserts, updates and deletes) that are sent to the database at one time for execution.
         */
        @ConfigItem
        public Optional<Integer> statementBatchSize;

        public boolean isAnyPropertySet() {
            return timezone.isPresent() || statementFetchSize.isPresent() || statementBatchSize.isPresent();
        }
    }

    @ConfigGroup
    public static class HibernateOrmConfigLog {

        /**
         * Show SQL logs and format them nicely.
         * <p>
         * Setting it to true is not recommended in production.
         */
        @ConfigItem(defaultValue = "false")
        public boolean sql;

        /**
         * Whether JDBC warnings should be collected and logged.
         * <p>
         * Default value depends on the dialect.
         */
        @ConfigItem
        public Optional<Boolean> jdbcWarnings;

        public boolean isAnyPropertySet() {
            return sql || jdbcWarnings.isPresent();
        }
    }

    @ConfigGroup
    public static class HibernateOrmConfigCache {
        /**
         * The cache expiration configuration.
         */
        @ConfigItem
        public HibernateOrmConfigCacheExpiration expiration;

        /**
         * The cache memory storage configuration.
         */
        @ConfigItem
        public HibernateOrmConfigCacheMemory memory;
    }

    @ConfigGroup
    public static class HibernateOrmConfigCacheExpiration {
        /**
         * The maximum time before an object is considered expired.
         * <p>
         * To set the maximum idle time, provide the duration (see note on duration’s format below) via the
         * {@code quarkus.hibernate-orm.cache."<region_name>".expiration.max-idle} (quotes mandatory).
         * <p>
         * The format for durations uses the standard {@code java.time.Duration} format.
         * You can learn more about it in the <a target="_top" href=
         * "https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-">Duration#parse()</a>
         * javadoc.
         * You can also provide duration values starting with a number.
         * In this case, if the value consists only of a number, the converter treats the value as seconds.
         * Otherwise, {@code PT} is implicitly appended to the value to obtain a standard {@code java.time.Duration} format.
         */
        @ConfigItem
        public Optional<Duration> maxIdle;
    }

    @ConfigGroup
    public static class HibernateOrmConfigCacheMemory {
        /**
         * The maximum number of objects kept in memory.
         * <p>
         * {@code quarkus.hibernate-orm.cache."<region_name>".object-count} (quotes mandatory).
         */
        @ConfigItem
        public OptionalLong objectCount;
    }
}
