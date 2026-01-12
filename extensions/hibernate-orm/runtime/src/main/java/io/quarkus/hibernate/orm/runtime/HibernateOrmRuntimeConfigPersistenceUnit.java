package io.quarkus.hibernate.orm.runtime;

import java.util.Map;
import java.util.Optional;

import jakarta.persistence.FlushModeType;

import org.hibernate.FlushMode;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigGroup
public interface HibernateOrmRuntimeConfigPersistenceUnit {

    /**
     * Whether this persistence unit should be active at runtime.
     *
     * See xref:hibernate-orm.adoc#persistence-unit-active[this section of the documentation].
     *
     * Note that if Hibernate ORM is disabled (i.e. `quarkus.hibernate-orm.enabled` is set to `false`),
     * all persistence units are deactivated, and setting this property to `true` will fail.
     *
     * @asciidoclet
     */
    @ConfigDocDefault("`true` if Hibernate ORM is enabled and there are entity types or an active datasource assigned to the persistence unit; `false` otherwise")
    Optional<Boolean> active();

    /**
     * Schema management configuration.
     */
    @ConfigDocSection
    HibernateOrmConfigPersistenceUnitSchemaManagement schemaManagement();

    /**
     * Database related configuration.
     */
    @ConfigDocSection
    HibernateOrmConfigPersistenceUnitDatabase database();

    /**
     * Database scripts related configuration.
     */
    @ConfigDocSection
    HibernateOrmConfigPersistenceUnitScripts scripts();

    /**
     * Logging configuration.
     */
    @ConfigDocSection
    HibernateOrmConfigPersistenceUnitLog log();

    /**
     * Flush configuration.
     */
    @ConfigDocSection
    HibernateOrmConfigPersistenceUnitFlush flush();

    /**
     * Properties that should be passed on directly to Hibernate ORM.
     * Use the full configuration property key here,
     * for instance `quarkus.hibernate-orm.unsupported-properties."hibernate.order_inserts" = true`.
     *
     * [WARNING]
     * ====
     * Properties set here are completely unsupported:
     * as Quarkus doesn't generally know about these properties and their purpose,
     * there is absolutely no guarantee that they will work correctly,
     * and even if they do, that may change when upgrading to a newer version of Quarkus
     * (even just a micro/patch version).
     * ====
     *
     * Consider using a supported configuration property before falling back to unsupported ones.
     * If none exists, make sure to file a feature request so that a supported configuration property can be added to Quarkus,
     * and more importantly so that the configuration property is tested regularly.
     *
     * @asciidoclet
     */
    @ConfigDocMapKey("full-property-key")
    Map<String, String> unsupportedProperties();

    @ConfigGroup
    interface HibernateOrmConfigPersistenceUnitDatabase {

        /**
         * Schema generation configuration.
         */
        @Deprecated(forRemoval = true, since = "3.22")
        HibernateOrmConfigPersistenceUnitDatabaseGeneration generation();

        /**
         * The default catalog to use for the database objects.
         */
        Optional<@WithConverter(TrimmedStringConverter.class) String> defaultCatalog();

        /**
         * The default schema to use for the database objects.
         */
        Optional<@WithConverter(TrimmedStringConverter.class) String> defaultSchema();

        /**
         * Whether Hibernate ORM should check on startup
         * that the version of the database matches the version configured on the dialect
         * (either the default version, or the one set through `quarkus.datasource.db-version`).
         *
         * This should be set to `false` if the database is not available on startup.
         *
         * @asciidoclet
         */
        @WithName("version-check.enabled")
        @ConfigDocDefault("`false` if starting offline (see `start-offline`), `true` otherwise")
        Optional<Boolean> versionCheckEnabled();

        /**
         * Instructs Hibernate ORM to avoid connecting to the database on startup.
         *
         * When starting offline:
         * * Hibernate ORM will not attempt to create a schema automatically, so it must already be created when the application
         * hits the database for the first time.
         * * Quarkus will not check that the database version matches the one configured at build time.
         *
         * @asciidoclet
         */
        @WithDefault("false")
        boolean startOffline();
    }

    @ConfigGroup
    interface HibernateOrmConfigPersistenceUnitScripts {

        /**
         * Schema generation configuration.
         */
        HibernateOrmConfigPersistenceUnitScriptGeneration generation();

    }

    @ConfigGroup
    interface HibernateOrmConfigPersistenceUnitSchemaManagement {

        /**
         * Select whether the database schema is generated or not.
         *
         * `drop-and-create` is awesome in development mode.
         *
         * This defaults to 'none'.
         *
         * However if Dev Services is in use and no other extensions that manage the schema are present
         * the value will be automatically overridden to 'drop-and-create'.
         *
         * Accepted values: `none`, `create`, `drop-and-create`, `drop`, `update`, `validate`.
         *
         * @asciidoclet
         */
        @WithDefault("none")
        HibernateGenerationStrategy strategy();

        /**
         * If Hibernate ORM should create the schemas automatically (for databases supporting them).
         */
        @WithDefault("false")
        boolean createSchemas();

        /**
         * Whether we should stop on the first error when applying the schema.
         */
        @WithDefault("false")
        boolean haltOnError();

        /**
         * Additional database object types to include in schema management operations.
         *
         * By default, Hibernate ORM only considers tables and sequences when performing
         * schema management operations.
         * This setting allows you to specify additional database object types that should be included,
         * such as "MATERIALIZED VIEW", "VIEW", or other database-specific object types.
         *
         * The exact supported values depend on the underlying database and dialect.
         *
         * @asciidoclet
         */
        Optional<@WithConverter(TrimmedStringConverter.class) String> extraPhysicalTableTypes();
    }

    enum HibernateGenerationStrategy {
        /**
         * No schema action.
         *
         * @asciidoclet
         */
        NONE("none"),
        /**
         * Create the schema.
         *
         * @asciidoclet
         */
        CREATE("create"),
        /**
         * Drop and then recreate the schema.
         *
         * @asciidoclet
         */
        DROP_AND_CREATE("drop-and-create"),
        /**
         * Drop the schema.
         *
         * @asciidoclet
         */
        DROP("drop"),
        /**
         * Update (alter) the database schema.
         *
         * @asciidoclet
         */
        UPDATE("update"),
        /**
         * Validate the database schema.
         *
         * @asciidoclet
         */
        VALIDATE("validate");

        private final String schemaGenerationString;

        HibernateGenerationStrategy(String schemaGenerationString) {
            this.schemaGenerationString = schemaGenerationString;
        }

        public static String getString(HibernateGenerationStrategy strategy) {
            return strategy.toString();
        }

        @Override
        public String toString() {
            return schemaGenerationString;
        }
    }

    @ConfigGroup
    @Deprecated(forRemoval = true, since = "3.22")
    interface HibernateOrmConfigPersistenceUnitDatabaseGeneration {

        /**
         * Select whether the database schema is generated or not.
         *
         * `drop-and-create` is awesome in development mode.
         *
         * This defaults to 'none', however if Dev Services is in use and no other extensions that manage the schema are present
         * this will default to 'drop-and-create'.
         *
         * Accepted values: `none`, `create`, `drop-and-create`, `drop`, `update`, `validate`.
         */
        @WithParentName
        @Deprecated(forRemoval = true, since = "3.22")
        Optional<HibernateGenerationStrategy> generation();

        /**
         * If Hibernate ORM should create the schemas automatically (for databases supporting them).
         */
        @Deprecated(forRemoval = true, since = "3.22")
        Optional<Boolean> createSchemas();

        /**
         * Whether we should stop on the first error when applying the schema.
         */
        @Deprecated(forRemoval = true, since = "3.22")
        Optional<Boolean> haltOnError();
    }

    @ConfigGroup
    interface HibernateOrmConfigPersistenceUnitScriptGeneration {

        /**
         * Select whether the database schema DDL files are generated or not.
         *
         * Accepted values: `none`, `create`, `drop-and-create`, `drop`, `update`, `validate`.
         */
        @WithParentName
        @WithDefault("none")
        HibernateGenerationStrategy generation();

        /**
         * Filename or URL where the database create DDL file should be generated.
         */
        Optional<@WithConverter(TrimmedStringConverter.class) String> createTarget();

        /**
         * Filename or URL where the database drop DDL file should be generated.
         */
        Optional<@WithConverter(TrimmedStringConverter.class) String> dropTarget();

    }

    @ConfigGroup
    interface HibernateOrmConfigPersistenceUnitLog {

        /**
         * Show SQL logs and format them nicely.
         * <p>
         * Setting it to true is obviously not recommended in production.
         */
        @WithDefault("false")
        boolean sql();

        /**
         * Format the SQL logs if SQL log is enabled
         */
        @WithDefault("true")
        boolean formatSql();

        /**
         * Highlight the SQL logs if SQL log is enabled
         */
        @WithDefault("true")
        boolean highlightSql();

        /**
         * Whether JDBC warnings should be collected and logged.
         */
        @ConfigDocDefault("depends on dialect")
        Optional<Boolean> jdbcWarnings();

        /**
         * If set, Hibernate will log queries that took more than specified number of milliseconds to execute.
         */
        Optional<Long> queriesSlowerThanMs();

    }

    @ConfigGroup
    interface HibernateOrmConfigPersistenceUnitFlush {
        /**
         * The default flushing strategy, or when to flush entities to the database in a Hibernate session:
         * before every query, on commit, ...
         *
         * This default can be overridden on a per-session basis with `Session#setHibernateFlushMode()`
         * or on a per-query basis with the hint `HibernateHints#HINT_FLUSH_MODE`.
         *
         * See the javadoc of `org.hibernate.FlushMode` for details.
         *
         * @asciidoclet
         */
        @WithDefault("auto")
        HibernateFlushMode mode();
    }

    enum HibernateFlushMode {
        /**
         * The `org.hibernate.Session` is only flushed when `org.hibernate.Session#flush()`
         * is called explicitly. This mode is very efficient for read-only
         * transactions.
         *
         * @asciidoclet
         */
        MANUAL,
        /**
         * The `org.hibernate.Session` is flushed when `org.hibernate.Transaction#commit()`
         * is called. It is never automatically flushed before query
         * execution.
         *
         * @see FlushModeType#COMMIT
         * @asciidoclet
         */
        COMMIT,
        /**
         * The `org.hibernate.Session` is flushed when `org.hibernate.Transaction#commit()`
         * is called, and is sometimes flushed before query execution in
         * order to ensure that queries never return stale state. This is
         * the default flush mode.
         *
         * @see FlushModeType#AUTO
         * @asciidoclet
         */
        AUTO,
        /**
         * The `org.hibernate.Session` is flushed when `org.hibernate.Transaction#commit()`
         * is called and before every query. This is usually unnecessary and
         * inefficient.
         *
         * @asciidoclet
         */
        ALWAYS;

        public FlushMode getHibernateFlushMode() {
            return FlushMode.valueOf(name());
        }
    }
}
