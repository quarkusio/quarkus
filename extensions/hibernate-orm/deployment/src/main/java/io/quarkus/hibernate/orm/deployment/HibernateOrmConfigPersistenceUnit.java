package io.quarkus.hibernate.orm.deployment;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.id.enhanced.StandardOptimizerDescriptor;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.TrimmedStringConverter;

@ConfigGroup
public class HibernateOrmConfigPersistenceUnit {

    /**
     * The name of the datasource which this persistence unit uses.
     * <p>
     * If undefined, it will use the default datasource.
     */
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<String> datasource;

    /**
     * The packages in which the entities affected to this persistence unit are located.
     */
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<Set<String>> packages;

    /**
     * Dialect related configuration.
     */
    @ConfigItem
    @ConfigDocSection
    public HibernateOrmConfigPersistenceUnitDialect dialect;

    // @formatter:off
    /**
     * Path to a file containing the SQL statements to execute when Hibernate ORM starts.
     *
     * The file is retrieved from the classpath resources,
     * so it must be located in the resources directory (e.g. `src/main/resources`).
     *
     * The default value for this setting differs depending on the Quarkus launch mode:
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
    @ConvertWith(TrimmedStringConverter.class)
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
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<String> physicalNamingStrategy;

    /**
     * Pluggable strategy for applying implicit naming rules when an explicit name is not given.
     *
     * Class name of the Hibernate ImplicitNamingStrategy implementation
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
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
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<String> metadataBuilderContributor;

    /**
     * XML files to configure the entity mapping, e.g. {@code META-INF/my-orm.xml}.
     * <p>
     * Defaults to `META-INF/orm.xml` if it exists.
     * Pass `no-file` to force Hibernate ORM to ignore `META-INF/orm.xml`.
     */
    @ConfigItem(defaultValueDocumentation = "META-INF/orm.xml if it exists; no-file otherwise")
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<Set<String>> mappingFiles;

    /**
     * Mapping configuration.
     */
    @ConfigItem
    @ConfigDocSection
    public HibernateOrmConfigPersistenceUnitMapping mapping;

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
     * Identifiers can be quoted using one of the available strategies.
     * <p>
     * Set to {@code none} by default, meaning no identifiers will be quoted. If set to {@code all}, all identifiers and column
     * definitions will be quoted. Additionally, setting it to {@code all-except-column-definitions} will skip the column
     * definitions, which can usually be required when they exist, or else use the option {@code only-keywords} to quote only
     * identifiers deemed SQL keywords by the Hibernate ORM dialect.
     */
    @ConfigItem(defaultValue = "none", name = "quote-identifiers.strategy")
    public IdentifierQuotingStrategy identifierQuotingStrategy;

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
     * Bean Validation configuration.
     */
    @ConfigItem
    public HibernateOrmConfigPersistenceValidation validation;

    /**
     * Defines the method for multi-tenancy (DATABASE, NONE, SCHEMA). The complete list of allowed values is available in the
     * https://javadoc.io/doc/org.hibernate/hibernate-core/5.6.10.Final/org/hibernate/MultiTenancyStrategy.html[Hibernate ORM
     * JavaDoc].
     * The type DISCRIMINATOR is currently not supported. The default value is NONE (no multi-tenancy).
     *
     * @asciidoclet
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<String> multitenant;

    /**
     * Defines the name of the datasource to use in case of SCHEMA approach. The datasource of the persistence unit will be used
     * if not set.
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<String> multitenantSchemaDatasource;

    /**
     * If hibernate is not auto generating the schema, and Quarkus is running in development mode
     * then Quarkus will attempt to validate the database after startup and print a log message if
     * there are any problems.
     */
    @ConfigItem(defaultValue = "true")
    public boolean validateInDevMode;

    @ConfigItem(generateDocumentation = false)
    @ConfigDocMapKey("full-property-key")
    public Map<String, String> unsupportedProperties = new HashMap<>();

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
                mapping.isAnyPropertySet() ||
                query.isAnyPropertySet() ||
                database.isAnyPropertySet() ||
                jdbc.isAnyPropertySet() ||
                !cache.isEmpty() ||
                !secondLevelCachingEnabled ||
                multitenant.isPresent() ||
                multitenantSchemaDatasource.isPresent() ||
                fetch.isAnyPropertySet() ||
                discriminator.isAnyPropertySet() ||
                identifierQuotingStrategy != IdentifierQuotingStrategy.NONE ||
                !unsupportedProperties.isEmpty();
    }

    @ConfigGroup
    public static class HibernateOrmConfigPersistenceUnitDialect {

        /**
         * Class name of the Hibernate ORM dialect.
         *
         * The complete list of bundled dialects is available in the
         * https://docs.jboss.org/hibernate/stable/orm/javadocs/org/hibernate/dialect/package-summary.html[Hibernate ORM
         * JavaDoc].
         *
         * Setting the dialect directly is only recommended as a last resort:
         * most popular databases have a corresponding Quarkus extension,
         * allowing Quarkus to select the dialect automatically,
         * in which case you do not need to set the dialect at all,
         * though you may want to set
         * xref:datasource.adoc#quarkus-datasource_quarkus.datasource.db-version[`quarkus.datasource.db-version`] as
         * high as possible
         * to benefit from the best performance and latest features.
         *
         * If your database does not have a corresponding Quarkus extension,
         * you will need to set the dialect directly.
         * In that case, keep in mind that the JDBC driver and Hibernate ORM dialect
         * may not work properly in GraalVM native executables.
         *
         * @asciidoclet
         */
        @ConfigItem(name = ConfigItem.PARENT, defaultValueDocumentation = "selected automatically for most popular databases")
        @ConvertWith(TrimmedStringConverter.class)
        public Optional<String> dialect;

        /**
         * The storage engine to use when the dialect supports multiple storage engines.
         *
         * E.g. `MyISAM` or `InnoDB` for MySQL.
         *
         * @asciidoclet
         */
        @ConfigItem
        @ConvertWith(TrimmedStringConverter.class)
        public Optional<String> storageEngine;

        public boolean isAnyPropertySet() {
            return dialect.isPresent() || storageEngine.isPresent();
        }
    }

    /**
     * Mapping-related configuration.
     */
    @ConfigGroup
    public static class HibernateOrmConfigPersistenceUnitMapping {
        /**
         * Timezone configuration.
         */
        @ConfigItem
        public Timezone timezone;

        /**
         * Optimizer configuration.
         */
        @ConfigItem
        public Id id;

        @ConfigGroup
        public static class Timezone {
            /**
             * How to store timezones in the database by default
             * for properties of type `OffsetDateTime` and `ZonedDateTime`.
             *
             * This default may be overridden on a per-property basis using `@TimeZoneStorage`.
             *
             * NOTE: Properties of type `OffsetTime` are https://hibernate.atlassian.net/browse/HHH-16287[not affected by this
             * setting].
             *
             * `default`::
             * Equivalent to `native` if supported, `normalize-utc` otherwise.
             * `auto`::
             * Equivalent to `native` if supported, `column` otherwise.
             * `native`::
             * Stores the timestamp and timezone in a column of type `timestamp with time zone`.
             * +
             * Only available on some databases/dialects;
             * if not supported, an exception will be thrown during static initialization.
             * `column`::
             * Stores the timezone in a separate column next to the timestamp column.
             * +
             * Use `@TimeZoneColumn` on the relevant entity property to customize the timezone column.
             * `normalize-utc`::
             * Does not store the timezone, and loses timezone information upon persisting.
             * +
             * Instead, normalizes the value to a timestamp in the UTC timezone.
             * `normalize`::
             * Does not store the timezone, and loses timezone information upon persisting.
             * +
             * Instead, normalizes the value:
             * * upon persisting to the database, to a timestamp in the JDBC timezone
             * set through `quarkus.hibernate-orm.jdbc.timezone`,
             * or the JVM default timezone if not set.
             * * upon reading back from the database, to the JVM default timezone.
             * +
             * Use this to get the legacy behavior of Quarkus 2 / Hibernate ORM 5 or older.
             *
             * @asciidoclet
             */
            @ConfigItem(name = "default-storage", defaultValueDocumentation = "default")
            public Optional<TimeZoneStorageType> timeZoneDefaultStorage;
        }

        @ConfigGroup
        public static class Id {
            /**
             * Optimizer configuration.
             */
            @ConfigItem
            public Optimizer optimizer;

            @ConfigGroup
            public static class Optimizer {
                /**
                 * The optimizer to apply to identifier generators
                 * whose optimizer is not configured explicitly.
                 *
                 * Only relevant for table- and sequence-based identifier generators.
                 * Other generators, such as UUID-based generators, will ignore this setting.
                 *
                 * The optimizer is responsible for pooling new identifier values,
                 * in order to reduce the frequency of database calls to retrieve those values
                 * and thereby improve performance.
                 *
                 * @asciidoclet
                 */
                @ConfigItem(name = "default", defaultValueDocumentation = "pooled-lo")
                // Note this needs to be a build-time property due to
                // org.hibernate.boot.internal.InFlightMetadataCollectorImpl.handleIdentifierValueBinding
                // which may call (indirectly) org.hibernate.id.enhanced.SequenceStructure.buildSequence
                // whose output depends on org.hibernate.id.enhanced.SequenceStructure.applyIncrementSizeToSourceValues
                // which is determined by the optimizer.
                public Optional<IdOptimizerType> idOptimizerDefault;
            }
        }

        public boolean isAnyPropertySet() {
            return timezone.timeZoneDefaultStorage.isPresent()
                    || id.optimizer.idOptimizerDefault.isPresent();
        }

    }

    public enum IdOptimizerType {
        /**
         * Assumes the value retrieved from the table/sequence is the lower end of the pool.
         *
         * Upon retrieving value `N`, the new pool of identifiers will go from `N` to `N + <allocation size> - 1`, inclusive.
         * `pooled`::
         * Assumes the value retrieved from the table/sequence is the higher end of the pool.
         * +
         * Upon retrieving value `N`, the new pool of identifiers will go from `N - <allocation size>` to `N + <allocation size>
         * - 1`, inclusive.
         * +
         * The first value, `1`, is handled differently to avoid negative identifiers.
         * +
         * Use this to get the legacy behavior of Quarkus 2 / Hibernate ORM 5 or older.
         * `none`::
         * No optimizer, resulting in a database call each and every time an identifier value is needed from the generator.
         * +
         * Not recommended in production environments:
         * may result in degraded performance and/or frequent gaps in identifier values.
         *
         * @asciidoclet
         */
        POOLED_LO(StandardOptimizerDescriptor.POOLED_LO),
        /**
         * Assumes the value retrieved from the table/sequence is the higher end of the pool.
         *
         * Upon retrieving value `N`, the new pool of identifiers will go from `N - <allocation size>` to `N + <allocation size>
         * - 1`, inclusive.
         *
         * The first value, `1`, is handled differently to avoid negative identifiers.
         *
         * Use this to get the legacy behavior of Quarkus 2 / Hibernate ORM 5 or older.
         *
         * @asciidoclet
         */
        POOLED(StandardOptimizerDescriptor.POOLED),
        /**
         * No optimizer, resulting in a database call each and every time an identifier value is needed from the generator.
         *
         * Not recommended in production environments:
         * may result in degraded performance and/or frequent gaps in identifier values.
         *
         * @asciidoclet
         */
        NONE(StandardOptimizerDescriptor.NONE);

        public final String configName;

        IdOptimizerType(StandardOptimizerDescriptor delegate) {
            configName = delegate.getExternalName();
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
         * see #{@value org.hibernate.cfg.AvailableSettings#QUERY_PLAN_CACHE_MAX_SIZE}
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

        /**
         * Enables IN clause parameter padding which improves statement caching.
         */
        @ConfigItem(defaultValue = "true")
        public boolean inClauseParameterPadding;

        public boolean isAnyPropertySet() {
            return queryPlanCacheMaxSize != DEFAULT_QUERY_PLAN_CACHE_MAX_SIZE
                    || defaultNullOrdering != NullOrdering.NONE
                    || !inClauseParameterPadding;
        }
    }

    @ConfigGroup
    public static class HibernateOrmConfigPersistenceUnitDatabase {

        private static final String DEFAULT_CHARSET = "UTF-8";

        /**
         * The charset of the database.
         * <p>
         * Used for DDL generation and also for the SQL import scripts.
         */
        @ConfigItem(defaultValue = DEFAULT_CHARSET)
        public Charset charset;

        /**
         * Whether Hibernate should quote all identifiers.
         *
         * @deprecated {@link #identifierQuotingStrategy} should be used to configure quoting strategy.
         */
        @ConfigItem
        @Deprecated
        public boolean globallyQuotedIdentifiers;

        public boolean isAnyPropertySet() {
            return !DEFAULT_CHARSET.equals(charset.name())
                    || globallyQuotedIdentifiers;
        }
    }

    @ConfigGroup
    public static class HibernateOrmConfigPersistenceUnitJdbc {

        /**
         * The time zone pushed to the JDBC driver.
         *
         * See `quarkus.hibernate-orm.mapping.timezone.default-storage`.
         */
        @ConfigItem
        @ConvertWith(TrimmedStringConverter.class)
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

    public enum IdentifierQuotingStrategy {
        NONE,
        ALL,
        ALL_EXCEPT_COLUMN_DEFINITIONS,
        ONLY_KEYWORDS
    }

    @ConfigGroup
    public static class HibernateOrmConfigPersistenceValidation {

        /**
         * Enables the Bean Validation integration.
         */
        @ConfigItem(defaultValue = "true")
        public boolean enabled;
    }

}
