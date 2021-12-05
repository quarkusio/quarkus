package io.quarkus.hibernate.orm.deployment;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

import org.hibernate.engine.query.spi.QueryPlanCache;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class HibernateOrmConfigPersistenceUnit {

    /**
     * The name of the datasource which this persistence unit uses.
     * <p>
     * If undefined, it will use the default datasource.
     */
    public Optional<String> datasource;

    /**
     * The packages in which the entities affected to this persistence unit are located.
     */
    public Optional<Set<String>> packages;

    /**
     * Dialect related configuration.
     */
    @ConfigItem
    @ConfigDocSection
    public HibernateOrmConfigPersistenceUnitDialect dialect;

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
    public Optional<List<String>> sqlLoadScript;

    /**
     * The size of the batches used when loading entities and collections.
     *
     * `-1` means batch loading is disabled.
     *
     * @deprecated {@link #fetch} should be used to configure fetching properties.
     * @asciidoclet
     */
    @ConfigItem(defaultValueDocumentation = "16")
    @Deprecated
    public OptionalInt batchFetchSize;

    /**
     * The maximum depth of outer join fetch tree for single-ended associations (one-to-one, many-to-one).
     *
     * A `0` disables default outer join fetching.
     *
     * @deprecated {@link #fetch} should be used to configure fetching properties.
     * @asciidoclet
     */
    @ConfigItem
    @Deprecated
    public OptionalInt maxFetchDepth;

    /**
     * Pluggable strategy contract for applying physical naming rules for database object names.
     *
     * Class name of the Hibernate PhysicalNamingStrategy implementation
     */
    @ConfigItem
    public Optional<String> physicalNamingStrategy;

    /**
     * Pluggable strategy for applying implicit naming rules when an explicit name is not given.
     *
     * Class name of the Hibernate ImplicitNamingStrategy implementation
     */
    @ConfigItem
    public Optional<String> implicitNamingStrategy;

    /**
     * Class name of a custom
     * https://docs.jboss.org/hibernate/stable/orm/javadocs/org/hibernate/boot/spi/MetadataBuilderContributor.html[`org.hibernate.boot.spi.MetadataBuilderContributor`]
     * implementation.
     *
     * [NOTE]
     * ====
     * Not all customization options exposed by
     * https://docs.jboss.org/hibernate/stable/orm/javadocs/org/hibernate/boot/MetadataBuilder.html[`org.hibernate.boot.MetadataBuilder`]
     * will work correctly. Stay clear of options related to classpath scanning in particular.
     *
     * This setting is exposed mainly to allow registration of types, converters and SQL functions.
     * ====
     *
     * @asciidoclet
     */
    @ConfigItem
    public Optional<String> metadataBuilderContributor;

    /**
     * XML files to configure the entity mapping, e.g. {@code META-INF/my-orm.xml}.
     * <p>
     * Defaults to `META-INF/orm.xml` if it exists.
     * Pass `no-file` to force Hibernate ORM to ignore `META-INF/orm.xml`.
     */
    @ConfigItem(defaultValueDocumentation = "META-INF/orm.xml if it exists; no-file otherwise")
    public Optional<Set<String>> mappingFiles;

    /**
     * Query related configuration.
     */
    @ConfigItem
    @ConfigDocSection
    public HibernateOrmConfigPersistenceUnitQuery query;

    /**
     * Database related configuration.
     */
    @ConfigItem
    @ConfigDocSection
    public HibernateOrmConfigPersistenceUnitDatabase database;

    /**
     * JDBC related configuration.
     */
    @ConfigItem
    @ConfigDocSection
    public HibernateOrmConfigPersistenceUnitJdbc jdbc;

    /**
     * Fetching logic configuration.
     */
    @ConfigItem
    @ConfigDocSection
    public HibernateOrmConfigPersistenceUnitFetch fetch;

    /**
     * Caching configuration
     */
    @ConfigDocSection
    public Map<String, HibernateOrmConfigPersistenceUnitCache> cache;

    /**
     * Discriminator related configuration.
     */
    @ConfigItem
    @ConfigDocSection
    public HibernateOrmConfigPersistenceUnitDiscriminator discriminator;

    /**
     * The default in Quarkus is for 2nd level caching to be enabled,
     * and a good implementation is already integrated for you.
     * <p>
     * Just cherry-pick which entities should be using the cache.
     * <p>
     * Set this to false to disable all 2nd level caches.
     */
    @ConfigItem(defaultValue = "true")
    public boolean secondLevelCachingEnabled;

    /**
     * Defines the method for multi-tenancy (DATABASE, NONE, SCHEMA). The complete list of allowed values is available in the
     * https://docs.jboss.org/hibernate/stable/orm/javadocs/org/hibernate/MultiTenancyStrategy.html[Hibernate ORM JavaDoc].
     * The type DISCRIMINATOR is currently not supported. The default value is NONE (no multi-tenancy).
     *
     * @asciidoclet
     */
    @ConfigItem
    public Optional<String> multitenant;

    /**
     * Defines the name of the datasource to use in case of SCHEMA approach. The datasource of the persistence unit will be used
     * if not set.
     */
    @ConfigItem
    public Optional<String> multitenantSchemaDatasource;

    /**
     * If hibernate is not auto generating the schema, and Quarkus is running in development mode
     * then Quarkus will attempt to validate the database after startup and print a log message if
     * there are any problems.
     */
    @ConfigItem(defaultValue = "true")
    public boolean validateInDevMode;

    public boolean isAnyPropertySet() {
        return datasource.isPresent() ||
                packages.isPresent() ||
                dialect.isAnyPropertySet() ||
                sqlLoadScript.isPresent() ||
                batchFetchSize.isPresent() ||
                maxFetchDepth.isPresent() ||
                physicalNamingStrategy.isPresent() ||
                implicitNamingStrategy.isPresent() ||
                metadataBuilderContributor.isPresent() ||
                query.isAnyPropertySet() ||
                database.isAnyPropertySet() ||
                jdbc.isAnyPropertySet() ||
                !cache.isEmpty() ||
                !secondLevelCachingEnabled ||
                multitenant.isPresent() ||
                multitenantSchemaDatasource.isPresent() ||
                fetch.isAnyPropertySet() ||
                discriminator.isAnyPropertySet();
    }

    @ConfigGroup
    public static class HibernateOrmConfigPersistenceUnitDialect {

        /**
         * Class name of the Hibernate ORM dialect. The complete list of bundled dialects is available in the
         * https://docs.jboss.org/hibernate/stable/orm/javadocs/org/hibernate/dialect/package-summary.html[Hibernate ORM
         * JavaDoc].
         *
         * [NOTE]
         * ====
         * Not all the dialects are supported in GraalVM native executables: we currently provide driver extensions for
         * PostgreSQL,
         * MariaDB, Microsoft SQL Server and H2.
         * ====
         *
         * @asciidoclet
         */
        // TODO should it be dialects
        //TODO should it be shortcuts like "postgresql" "h2" etc
        @ConfigItem(name = ConfigItem.PARENT)
        public Optional<String> dialect;

        /**
         * The storage engine to use when the dialect supports multiple storage engines.
         *
         * E.g. `MyISAM` or `InnoDB` for MySQL.
         *
         * @asciidoclet
         */
        @ConfigItem
        public Optional<String> storageEngine;

        public boolean isAnyPropertySet() {
            return dialect.isPresent() || storageEngine.isPresent();
        }
    }

    @ConfigGroup
    public static class HibernateOrmConfigPersistenceUnitQuery {

        private static final int DEFAULT_QUERY_PLAN_CACHE_MAX_SIZE = 2048;

        public enum NullOrdering {
            NONE,
            FIRST,
            LAST
        }

        /**
         * The maximum size of the query plan cache.
         * see #{@value QueryPlanCache#DEFAULT_QUERY_PLAN_MAX_COUNT}
         */
        @ConfigItem(defaultValue = "2048")
        public int queryPlanCacheMaxSize;

        /**
         * Default precedence of null values in `ORDER BY` clauses.
         *
         * Valid values are: `none`, `first`, `last`.
         *
         * @asciidoclet
         */
        @ConfigItem(defaultValue = "none")
        public NullOrdering defaultNullOrdering;

        public boolean isAnyPropertySet() {
            return queryPlanCacheMaxSize != DEFAULT_QUERY_PLAN_CACHE_MAX_SIZE
                    || defaultNullOrdering != NullOrdering.NONE;
        }
    }

    @ConfigGroup
    public static class HibernateOrmConfigPersistenceUnitDatabase {

        private static final String DEFAULT_CHARSET = "UTF-8";

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
         * <p>
         * Used for DDL generation and also for the SQL import scripts.
         */
        @ConfigItem(defaultValue = DEFAULT_CHARSET)
        public Charset charset;

        /**
         * Whether Hibernate should quote all identifiers.
         */
        @ConfigItem
        public boolean globallyQuotedIdentifiers;

        public boolean isAnyPropertySet() {
            return defaultCatalog.isPresent()
                    || defaultSchema.isPresent()
                    || !DEFAULT_CHARSET.equals(charset.name())
                    || globallyQuotedIdentifiers;
        }
    }

    @ConfigGroup
    public static class HibernateOrmConfigPersistenceUnitJdbc {

        /**
         * The time zone pushed to the JDBC driver.
         */
        @ConfigItem
        public Optional<String> timezone;

        /**
         * How many rows are fetched at a time by the JDBC driver.
         */
        @ConfigItem
        public OptionalInt statementFetchSize;

        /**
         * The number of updates (inserts, updates and deletes) that are sent by the JDBC driver at one time for execution.
         */
        @ConfigItem
        public OptionalInt statementBatchSize;

        public boolean isAnyPropertySet() {
            return timezone.isPresent() || statementFetchSize.isPresent() || statementBatchSize.isPresent();
        }
    }

    @ConfigGroup
    public static class HibernateOrmConfigPersistenceUnitCache {
        /**
         * The cache expiration configuration.
         */
        @ConfigItem
        public HibernateOrmConfigPersistenceUnitCacheExpiration expiration;

        /**
         * The cache memory storage configuration.
         */
        @ConfigItem
        public HibernateOrmConfigPersistenceUnitCacheMemory memory;
    }

    @ConfigGroup
    public static class HibernateOrmConfigPersistenceUnitCacheExpiration {
        /**
         * The maximum time before an object of the cache is considered expired.
         */
        @ConfigItem
        public Optional<Duration> maxIdle;
    }

    @ConfigGroup
    public static class HibernateOrmConfigPersistenceUnitCacheMemory {
        /**
         * The maximum number of objects kept in memory in the cache.
         */
        @ConfigItem
        public OptionalLong objectCount;
    }

    @ConfigGroup
    public static class HibernateOrmConfigPersistenceUnitFetch {
        /**
         * The size of the batches used when loading entities and collections.
         *
         * `-1` means batch loading is disabled.
         *
         * @asciidoclet
         */
        @ConfigItem(defaultValueDocumentation = "16")
        public OptionalInt batchSize;

        /**
         * The maximum depth of outer join fetch tree for single-ended associations (one-to-one, many-to-one).
         *
         * A `0` disables default outer join fetching.
         *
         * @asciidoclet
         */
        @ConfigItem
        public OptionalInt maxDepth;

        public boolean isAnyPropertySet() {
            return batchSize.isPresent() || maxDepth.isPresent();
        }

    }

    /**
     * Discriminator configuration.
     *
     * Separated in a group configuration, in case it is necessary to add the another existing hibernate discriminator property.
     */
    @ConfigGroup
    public static class HibernateOrmConfigPersistenceUnitDiscriminator {
        /**
         * Existing applications rely (implicitly or explicitly) on Hibernate ignoring any DiscriminatorColumn declarations on
         * joined inheritance hierarchies. This setting allows these applications to maintain the legacy behavior of
         * DiscriminatorColumn annotations being ignored when paired with joined inheritance.
         */
        @ConfigItem
        public boolean ignoreExplicitForJoined;

        public boolean isAnyPropertySet() {
            return ignoreExplicitForJoined;
        }
    }
}