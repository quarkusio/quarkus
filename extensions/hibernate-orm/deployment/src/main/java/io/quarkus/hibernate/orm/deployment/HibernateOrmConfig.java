package io.quarkus.hibernate.orm.deployment;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class HibernateOrmConfig {
    /**
     * Class name of the Hibernate ORM dialect. The complete list of bundled dialects is available in the
     * https://docs.jboss.org/hibernate/stable/orm/javadocs/org/hibernate/dialect/package-summary.html[Hibernate ORM JavaDoc].
     *
     * [NOTE]
     * ====
     * Not all the dialects are supported in GraalVM native executables: we currently provide driver extensions for PostgreSQL,
     * MariaDB, Microsoft SQL Server and H2.
     * ====
     *
     * @asciidoclet
     */
    // TODO should it be dialects
    //TODO should it be shortcuts like "postgresql" "h2" etc
    @ConfigItem
    public Optional<String> dialect;

    /**
     * The storage engine to use when the dialect supports multiple storage engines.
     *
     * E.g. `MyISAM` or `InnoDB` for MySQL.
     *
     * @asciidoclet
     */
    @ConfigItem(name = "dialect.storage-engine")
    public Optional<String> dialectStorageEngine;

    // @formatter:off
    /**
     * Name of the file containing the SQL statements to execute when Hibernate ORM starts.
     * Its default value differs depending on the Quarkus launch mode:
     *
     * * In dev and test modes, it defaults to `import.sql`.
     *   Simply add an `import.sql` file in the root of your resources directory
     *   and it will be picked up without having to set this property.
     *   Pass `no-file` to force Hibernate ORM to ignore the SQL import file.
     * * In production mode, it defaults to `no-file`.
     *   It means Hibernate ORM won't try to execute any SQL import file by default.
     *   Pass an explicit value to force Hibernate ORM to execute the SQL import file.
     *
     * If you need different SQL statements between dev mode, test (`@QuarkusTest`) and in production, use Quarkus
     * https://quarkus.io/guides/config#configuration-profiles[configuration profiles facility].
     *
     * [source,property]
     * .application.properties
     * ----
     * %dev.quarkus.hibernate-orm.sql-load-script = import-dev.sql
     * %test.quarkus.hibernate-orm.sql-load-script = import-test.sql
     * %prod.quarkus.hibernate-orm.sql-load-script = no-file
     * ----
     *
     * [NOTE]
     * ====
     * Quarkus supports `.sql` file with SQL statements or comments spread over multiple lines.
     * Each SQL statement must be terminated by a semicolon.
     * ====
     *
     * @asciidoclet
     */
    // @formatter:on
    @ConfigItem(defaultValueDocumentation = "import.sql in DEV, TEST ; no-file otherwise")
    public Optional<String> sqlLoadScript;

    /**
     * The size of the batches used when loading entities and collections.
     *
     * `-1` means batch loading is disabled. This is the default.
     *
     * @asciidoclet
     */
    @ConfigItem(defaultValue = "-1")
    public int batchFetchSize;

    /**
     * Pluggable strategy contract for applying physical naming rules for database object names.
     *
     * Class name of the Hibernate PhysicalNamingStrategy implementation
     */
    @ConfigItem
    Optional<String> physicalNamingStrategy;

    /**
     * Pluggable strategy for applying implicit naming rules when an explicit name is not given.
     *
     * Class name of the Hibernate ImplicitNamingStrategy implementation
     */
    @ConfigItem
    Optional<String> implicitNamingStrategy;

    /**
     * Query related configuration.
     */
    @ConfigItem
    @ConfigDocSection
    public HibernateOrmConfigQuery query;

    /**
     * Database related configuration.
     */
    @ConfigItem
    @ConfigDocSection
    public HibernateOrmConfigDatabase database;

    /**
     * JDBC related configuration.
     */
    @ConfigItem
    @ConfigDocSection
    public HibernateOrmConfigJdbc jdbc;

    /**
     * Logging configuration.
     */
    @ConfigItem
    @ConfigDocSection
    public HibernateOrmConfigLog log;

    /**
     * Caching configuration
     */
    @ConfigDocSection
    public Map<String, HibernateOrmConfigCache> cache;

    /**
     * Whether statistics collection is enabled. If 'metrics.enabled' is true, then the default here is
     * considered true, otherwise the default is false.
     */
    @ConfigItem
    public Optional<Boolean> statistics;

    /**
     * Whether or not metrics are published in case the smallrye-metrics extension is present (default to false).
     */
    @ConfigItem(name = "metrics.enabled", defaultValue = "false")
    public boolean metricsEnabled;

    public boolean isAnyPropertySet() {
        return dialect.isPresent() ||
                dialectStorageEngine.isPresent() ||
                sqlLoadScript.isPresent() ||
                batchFetchSize > 0 ||
                statistics.isPresent() ||
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
         * Default precedence of null values in `ORDER BY` clauses.
         *
         * Valid values are: `none`, `first`, `last`.
         *
         * @asciidoclet
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
         *
         * `drop-and-create` is awesome in development mode.
         *
         * Accepted values: `none`, `create`, `drop-and-create`, `drop`, `update`.
         */
        @ConfigItem(defaultValue = "none")
        public String generation;

        /**
         * Whether we should stop on the first error when applying the schema.
         */
        @ConfigItem(name = "generation.halt-on-error", defaultValue = "false")
        public boolean generationHaltOnError;

        /**
         * The default catalog to use for the database objects.
         */
        @ConfigItem
        public Optional<String> defaultCatalog;

        /**
         * The default schema to use for the database objects.
         */
        @ConfigItem
        public Optional<String> defaultSchema;

        /**
         * The charset of the database.
         */
        @ConfigItem
        public Optional<String> charset;

        /**
         * Whether Hibernate should quote all identifiers.
         */
        @ConfigItem(defaultValue = "false")
        public boolean globallyQuotedIdentifiers;

        public boolean isAnyPropertySet() {
            return !"none".equals(generation) || defaultCatalog.isPresent() || defaultSchema.isPresent()
                    || generationHaltOnError
                    || charset.isPresent()
                    || globallyQuotedIdentifiers;
        }
    }

    @ConfigGroup
    public static class HibernateOrmConfigJdbc {

        /**
         * The time zone pushed to the JDBC driver.
         */
        @ConfigItem
        public Optional<String> timezone;

        /**
         * How many rows are fetched at a time by the JDBC driver.
         */
        @ConfigItem
        public Optional<Integer> statementFetchSize;

        /**
         * The number of updates (inserts, updates and deletes) that are sent by the JDBC driver at one time for execution.
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
         * Setting it to true is obviously not recommended in production.
         */
        @ConfigItem(defaultValue = "false")
        public boolean sql;

        /**
         * Whether JDBC warnings should be collected and logged.
         */
        @ConfigItem(defaultValueDocumentation = "depends on dialect")
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
         * The maximum time before an object of the cache is considered expired.
         */
        @ConfigItem
        public Optional<Duration> maxIdle;
    }

    @ConfigGroup
    public static class HibernateOrmConfigCacheMemory {
        /**
         * The maximum number of objects kept in memory in the cache.
         */
        @ConfigItem
        public OptionalLong objectCount;
    }
}
