package io.quarkus.hibernate.orm.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigGroup
public interface HibernateOrmRuntimeConfigPersistenceUnit {

    /**
     * Whether this persistence unit should be active at runtime.
     *
     * See xref:hibernate-orm.adoc#persistence-unit-active[this section of the documentation].
     *
     * If the persistence unit is not active, it won't start with the application,
     * and accessing the corresponding EntityManagerFactory/EntityManager or SessionFactory/Session
     * will not be possible.
     *
     * Note that if Hibernate ORM is disabled (i.e. `quarkus.hibernate-orm.enabled` is set to `false`),
     * all persistence units are deactivated, and setting this property to `true` will fail.
     *
     * @asciidoclet
     */
    @ConfigDocDefault("'true' if Hibernate ORM is enabled; 'false' otherwise")
    Optional<Boolean> active();

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
        HibernateOrmConfigPersistenceUnitDatabaseGeneration generation();

        /**
         * The default catalog to use for the database objects.
         */
        @WithConverter(TrimmedStringConverter.class)
        Optional<String> defaultCatalog();

        /**
         * The default schema to use for the database objects.
         */
        @WithConverter(TrimmedStringConverter.class)
        Optional<String> defaultSchema();

    }

    @ConfigGroup
    interface HibernateOrmConfigPersistenceUnitScripts {

        /**
         * Schema generation configuration.
         */
        HibernateOrmConfigPersistenceUnitScriptGeneration generation();

    }

    @ConfigGroup
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
        @WithDefault("none")
        @WithConverter(TrimmedStringConverter.class)
        String generation();

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
        @WithConverter(TrimmedStringConverter.class)
        String generation();

        /**
         * Filename or URL where the database create DDL file should be generated.
         */
        @WithConverter(TrimmedStringConverter.class)
        Optional<String> createTarget();

        /**
         * Filename or URL where the database drop DDL file should be generated.
         */
        @WithConverter(TrimmedStringConverter.class)
        Optional<String> dropTarget();

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

}
