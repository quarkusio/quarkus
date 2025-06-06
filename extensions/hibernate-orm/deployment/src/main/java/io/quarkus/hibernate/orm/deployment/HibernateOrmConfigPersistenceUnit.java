package io.quarkus.hibernate.orm.deployment;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.id.enhanced.StandardOptimizerDescriptor;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocIgnore;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigGroup
public interface HibernateOrmConfigPersistenceUnit {

    /**
     * The name of the datasource which this persistence unit uses.
     * <p>
     * If undefined, it will use the default datasource.
     */
    Optional<@WithConverter(TrimmedStringConverter.class) String> datasource();

    /**
     * The packages in which the entities affected to this persistence unit are located.
     */
    Optional<Set<@WithConverter(TrimmedStringConverter.class) String>> packages();

    /**
     * Dialect related configuration.
     */
    @ConfigDocSection
    HibernateOrmConfigPersistenceUnitDialect dialect();

    // @formatter:off
    /**
     * Paths to files containing the SQL statements to execute when Hibernate ORM starts.
     *
     * The files are retrieved from the classpath resources,
     * so they must be located in the resources directory (e.g. `src/main/resources`).
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
    @ConfigDocDefault("import.sql in dev and test modes ; no-file otherwise")
    Optional<List<@WithConverter(TrimmedStringConverter.class) String>> sqlLoadScript();

    /**
     * The size of the batches used when loading entities and collections.
     *
     * `-1` means batch loading is disabled.
     *
     * @deprecated {@link #fetch} should be used to configure fetching properties.
     * @asciidoclet
     */
    @ConfigDocDefault("16")
    @Deprecated
    OptionalInt batchFetchSize();

    /**
     * The maximum depth of outer join fetch tree for single-ended associations (one-to-one, many-to-one).
     *
     * A `0` disables default outer join fetching.
     *
     * @deprecated {@link #fetch} should be used to configure fetching properties.
     * @asciidoclet
     */
    @Deprecated
    OptionalInt maxFetchDepth();

    /**
     * Pluggable strategy contract for applying physical naming rules for database object names.
     *
     * Class name of the Hibernate PhysicalNamingStrategy implementation
     */
    Optional<@WithConverter(TrimmedStringConverter.class) String> physicalNamingStrategy();

    /**
     * Pluggable strategy for applying implicit naming rules when an explicit name is not given.
     *
     * Class name of the Hibernate ImplicitNamingStrategy implementation
     */
    Optional<@WithConverter(TrimmedStringConverter.class) String> implicitNamingStrategy();

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
    Optional<@WithConverter(TrimmedStringConverter.class) String> metadataBuilderContributor();

    /**
     * XML files to configure the entity mapping, e.g. {@code META-INF/my-orm.xml}.
     * <p>
     * Defaults to `META-INF/orm.xml` if it exists.
     * Pass `no-file` to force Hibernate ORM to ignore `META-INF/orm.xml`.
     */
    @ConfigDocDefault("META-INF/orm.xml if it exists; no-file otherwise")
    Optional<Set<@WithConverter(TrimmedStringConverter.class) String>> mappingFiles();

    /**
     * Mapping configuration.
     */
    @ConfigDocSection
    HibernateOrmConfigPersistenceUnitMapping mapping();

    /**
     * Query related configuration.
     */
    @ConfigDocSection
    HibernateOrmConfigPersistenceUnitQuery query();

    /**
     * Database related configuration.
     */
    @ConfigDocSection
    HibernateOrmConfigPersistenceUnitDatabase database();

    /**
     * JDBC related configuration.
     */
    @ConfigDocSection
    HibernateOrmConfigPersistenceUnitJdbc jdbc();

    /**
     * Fetching logic configuration.
     */
    @ConfigDocSection
    HibernateOrmConfigPersistenceUnitFetch fetch();

    /**
     * Caching configuration
     */
    @ConfigDocSection
    Map<String, HibernateOrmConfigPersistenceUnitCache> cache();

    /**
     * Discriminator related configuration.
     */
    @ConfigDocSection
    HibernateOrmConfigPersistenceUnitDiscriminator discriminator();

    /**
     * Config related to identifier quoting.
     */
    HibernateOrmConfigPersistenceUnitQuoteIdentifiers quoteIdentifiers();

    /**
     * The default in Quarkus is for 2nd level caching to be enabled,
     * and a good implementation is already integrated for you.
     * <p>
     * Just cherry-pick which entities should be using the cache.
     * <p>
     * Set this to false to disable all 2nd level caches.
     */
    @WithDefault("true")
    boolean secondLevelCachingEnabled();

    /**
     * Bean Validation configuration.
     */
    HibernateOrmConfigPersistenceValidation validation();

    /**
     * Defines the method for multi-tenancy (DATABASE, NONE, SCHEMA). The complete list of allowed values is available in the
     * https://javadoc.io/doc/org.hibernate/hibernate-core/5.6.10.Final/org/hibernate/MultiTenancyStrategy.html[Hibernate ORM
     * JavaDoc].
     * The type DISCRIMINATOR is currently not supported. The default value is NONE (no multi-tenancy).
     *
     * @asciidoclet
     */
    Optional<@WithConverter(TrimmedStringConverter.class) String> multitenant();

    /**
     * Defines the name of the datasource to use in case of SCHEMA approach. The datasource of the persistence unit will be used
     * if not set.
     *
     * @deprecated Use {@link #datasource()} instead.
     */
    @Deprecated
    Optional<@WithConverter(TrimmedStringConverter.class) String> multitenantSchemaDatasource();

    /**
     * If hibernate is not auto generating the schema, and Quarkus is running in development mode
     * then Quarkus will attempt to validate the database after startup and print a log message if
     * there are any problems.
     */
    @WithDefault("true")
    boolean validateInDevMode();

    @ConfigDocIgnore
    @ConfigDocMapKey("full-property-key")
    Map<String, String> unsupportedProperties();

    default boolean isAnyPropertySet() {
        return datasource().isPresent() ||
                packages().isPresent() ||
                dialect().isAnyPropertySet() ||
                sqlLoadScript().isPresent() ||
                batchFetchSize().isPresent() ||
                maxFetchDepth().isPresent() ||
                physicalNamingStrategy().isPresent() ||
                implicitNamingStrategy().isPresent() ||
                metadataBuilderContributor().isPresent() ||
                mapping().isAnyPropertySet() ||
                query().isAnyPropertySet() ||
                database().isAnyPropertySet() ||
                jdbc().isAnyPropertySet() ||
                !cache().isEmpty() ||
                !secondLevelCachingEnabled() ||
                multitenant().isPresent() ||
                multitenantSchemaDatasource().isPresent() ||
                fetch().isAnyPropertySet() ||
                discriminator().isAnyPropertySet() ||
                quoteIdentifiers().isAnyPropertySet() ||
                !unsupportedProperties().isEmpty();
    }

    @ConfigGroup
    interface HibernateOrmConfigPersistenceUnitDialect {

        /**
         * Name of the Hibernate ORM dialect.
         *
         * For xref:datasource.adoc#extensions-and-database-drivers-reference[supported databases],
         * this property does not need to be set explicitly:
         * it is selected automatically based on the datasource,
         * and configured using the xref:datasource.adoc#quarkus-datasource_quarkus.datasource.db-version[DB version set on the
         * datasource]
         * to benefit from the best performance and latest features.
         *
         * If your database does not have a corresponding Quarkus extension,
         * you *will* need to set this property explicitly.
         * In that case, keep in mind that the JDBC driver and Hibernate ORM dialect
         * may not work properly in GraalVM native executables.
         *
         * For built-in dialects, the expected value is one of the names
         * in the link:{hibernate-orm-dialect-docs-url}[official list of dialects],
         * *without* the `Dialect` suffix,
         * for example `Cockroach` for `CockroachDialect`.
         *
         * For third-party dialects, the expected value is the fully-qualified class name,
         * for example `com.acme.hibernate.AcmeDbDialect`.
         *
         * @asciidoclet
         */
        @WithParentName
        @ConfigDocDefault("selected automatically for most popular databases")
        Optional<@WithConverter(TrimmedStringConverter.class) String> dialect();

        /**
         * The storage engine to use when the dialect supports multiple storage engines.
         *
         * E.g. `MyISAM` or `InnoDB` for MySQL.
         *
         * @asciidoclet
         */
        Optional<@WithConverter(TrimmedStringConverter.class) String> storageEngine();

        default boolean isAnyPropertySet() {
            return dialect().isPresent() || storageEngine().isPresent();
        }
    }

    /**
     * Mapping-related configuration.
     */
    @ConfigGroup
    interface HibernateOrmConfigPersistenceUnitMapping {
        /**
         * Timezone configuration.
         */
        Timezone timezone();

        /**
         * Optimizer configuration.
         */
        Id id();

        @ConfigGroup
        interface Timezone {
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
            @WithName("default-storage")
            @ConfigDocDefault("default")
            Optional<TimeZoneStorageType> timeZoneDefaultStorage();
        }

        @ConfigGroup
        interface Id {
            /**
             * Optimizer configuration.
             */
            Optimizer optimizer();

            @ConfigGroup
            interface Optimizer {
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
                @WithName("default")
                @ConfigDocDefault("pooled-lo")
                // Note this needs to be a build-time property due to
                // org.hibernate.boot.internal.InFlightMetadataCollectorImpl.handleIdentifierValueBinding
                // which may call (indirectly) org.hibernate.id.enhanced.SequenceStructure.buildSequence
                // whose output depends on org.hibernate.id.enhanced.SequenceStructure.applyIncrementSizeToSourceValues
                // which is determined by the optimizer.
                Optional<IdOptimizerType> idOptimizerDefault();
            }
        }

        default boolean isAnyPropertySet() {
            return timezone().timeZoneDefaultStorage().isPresent()
                    || id().optimizer().idOptimizerDefault().isPresent();
        }

    }

    enum IdOptimizerType {
        /**
         * Assumes the value retrieved from the table/sequence is the lower end of the pool.
         *
         * Upon retrieving value `N`, the new pool of identifiers will go from `N` to `N + <allocation size> - 1`, inclusive.
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
    interface HibernateOrmConfigPersistenceUnitQuery {

        int DEFAULT_QUERY_PLAN_CACHE_MAX_SIZE = 2048;

        enum NullOrdering {
            NONE,
            FIRST,
            LAST
        }

        /**
         * The maximum size of the query plan cache.
         * see #{@value org.hibernate.cfg.AvailableSettings#QUERY_PLAN_CACHE_MAX_SIZE}
         */
        @WithDefault("2048")
        int queryPlanCacheMaxSize();

        /**
         * Default precedence of null values in `ORDER BY` clauses.
         *
         * Valid values are: `none`, `first`, `last`.
         *
         * @asciidoclet
         */
        @WithDefault("none")
        NullOrdering defaultNullOrdering();

        /**
         * Enables IN clause parameter padding which improves statement caching.
         */
        @WithDefault("true")
        boolean inClauseParameterPadding();

        /**
         * When limits cannot be applied on the database side,
         * trigger an exception instead of attempting badly-performing in-memory result set limits.
         *
         * When pagination is used in combination with a fetch join applied to a collection or many-valued association,
         * the limit must be applied in-memory instead of on the database.
         * This should be avoided as it typically has terrible performance characteristics.
         *
         * @asciidoclet
         */
        @WithDefault("false")
        boolean failOnPaginationOverCollectionFetch();

        default boolean isAnyPropertySet() {
            return queryPlanCacheMaxSize() != DEFAULT_QUERY_PLAN_CACHE_MAX_SIZE
                    || defaultNullOrdering() != NullOrdering.NONE
                    || !inClauseParameterPadding();
        }
    }

    @ConfigGroup
    interface HibernateOrmConfigPersistenceUnitDatabase {

        String DEFAULT_CHARSET = "UTF-8";

        /**
         * The charset of the database.
         * <p>
         * Used for DDL generation and also for the SQL import scripts.
         */
        @WithDefault(DEFAULT_CHARSET)
        Charset charset();

        /**
         * Whether Hibernate should quote all identifiers.
         *
         * @deprecated {@link #quoteIdentifiers} should be used to configure quoting strategy.
         */
        @Deprecated
        @WithDefault("false")
        boolean globallyQuotedIdentifiers();

        default boolean isAnyPropertySet() {
            return !DEFAULT_CHARSET.equals(charset().name())
                    || globallyQuotedIdentifiers();
        }
    }

    @ConfigGroup
    interface HibernateOrmConfigPersistenceUnitJdbc {

        /**
         * The time zone pushed to the JDBC driver.
         *
         * See `quarkus.hibernate-orm.mapping.timezone.default-storage`.
         */
        Optional<@WithConverter(TrimmedStringConverter.class) String> timezone();

        /**
         * How many rows are fetched at a time by the JDBC driver.
         */
        OptionalInt statementFetchSize();

        /**
         * The number of updates (inserts, updates and deletes) that are sent by the JDBC driver at one time for execution.
         */
        OptionalInt statementBatchSize();

        default boolean isAnyPropertySet() {
            return timezone().isPresent() || statementFetchSize().isPresent() || statementBatchSize().isPresent();
        }
    }

    @ConfigGroup
    interface HibernateOrmConfigPersistenceUnitCache {
        /**
         * The cache expiration configuration.
         */
        HibernateOrmConfigPersistenceUnitCacheExpiration expiration();

        /**
         * The cache memory storage configuration.
         */
        HibernateOrmConfigPersistenceUnitCacheMemory memory();
    }

    @ConfigGroup
    interface HibernateOrmConfigPersistenceUnitCacheExpiration {
        /**
         * The maximum time before an object of the cache is considered expired.
         */
        Optional<Duration> maxIdle();
    }

    @ConfigGroup
    interface HibernateOrmConfigPersistenceUnitCacheMemory {
        /**
         * The maximum number of objects kept in memory in the cache.
         */
        OptionalLong objectCount();
    }

    @ConfigGroup
    interface HibernateOrmConfigPersistenceUnitFetch {
        /**
         * The size of the batches used when loading entities and collections.
         *
         * `-1` means batch loading is disabled.
         *
         * @asciidoclet
         */
        @ConfigDocDefault("16")
        OptionalInt batchSize();

        /**
         * The maximum depth of outer join fetch tree for single-ended associations (one-to-one, many-to-one).
         *
         * A `0` disables default outer join fetching.
         *
         * @asciidoclet
         */
        OptionalInt maxDepth();

        default boolean isAnyPropertySet() {
            return batchSize().isPresent() || maxDepth().isPresent();
        }

    }

    @ConfigGroup
    interface HibernateOrmConfigPersistenceUnitQuoteIdentifiers {

        /**
         * Identifiers can be quoted using one of the available strategies.
         * <p>
         * Set to {@code none} by default, meaning no identifiers will be quoted. If set to {@code all}, all identifiers and
         * column
         * definitions will be quoted. Additionally, setting it to {@code all-except-column-definitions} will skip the column
         * definitions, which can usually be required when they exist, or else use the option {@code only-keywords} to quote
         * only
         * identifiers deemed SQL keywords by the Hibernate ORM dialect.
         */
        @WithDefault("none")
        IdentifierQuotingStrategy strategy();

        default boolean isAnyPropertySet() {
            return strategy() != IdentifierQuotingStrategy.NONE;
        }
    }

    /**
     * Discriminator configuration.
     *
     * Separated in a group configuration, in case it is necessary to add the another existing hibernate discriminator property.
     */
    @ConfigGroup
    interface HibernateOrmConfigPersistenceUnitDiscriminator {
        /**
         * Existing applications rely (implicitly or explicitly) on Hibernate ignoring any DiscriminatorColumn declarations on
         * joined inheritance hierarchies. This setting allows these applications to maintain the legacy behavior of
         * DiscriminatorColumn annotations being ignored when paired with joined inheritance.
         */
        @WithDefault("false")
        boolean ignoreExplicitForJoined();

        default boolean isAnyPropertySet() {
            return ignoreExplicitForJoined();
        }
    }

    enum IdentifierQuotingStrategy {
        NONE,
        ALL,
        ALL_EXCEPT_COLUMN_DEFINITIONS,
        ONLY_KEYWORDS
    }

    @ConfigGroup
    interface HibernateOrmConfigPersistenceValidation {

        /**
         * Enables the Bean Validation integration.
         *
         * @deprecated Use {@link #mode()} instead.
         */
        @Deprecated(since = "3.19", forRemoval = true)
        @WithDefault("true")
        boolean enabled();

        /**
         * Defines how the Bean Validation integration behaves.
         */
        @WithDefault("auto")
        Set<ValidationMode> mode();

        enum ValidationMode {
            /**
             * If a Bean Validation provider is present then behaves as if both {@link ValidationMode#CALLBACK} and
             * {@link ValidationMode#DDL} modes are configured. Otherwise, same as {@link ValidationMode#NONE}.
             */
            AUTO,
            /**
             * Bean Validation will perform the lifecycle event validation.
             */
            CALLBACK,
            /**
             * Bean Validation constraints will be considered for the DDL operations.
             */
            DDL,
            /**
             * Bean Validation integration will be disabled.
             */
            NONE
        }
    }

}
