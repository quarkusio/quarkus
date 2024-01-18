package io.quarkus.hibernate.orm.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.TrimmedStringConverter;

@ConfigGroup
public class HibernateOrmRuntimeConfigPersistenceUnit {

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
    @ConfigItem(defaultValueDocumentation = "'true' if Hibernate ORM is enabled; 'false' otherwise")
    public Optional<Boolean> active = Optional.empty();

    /**
     * Database related configuration.
     */
    @ConfigItem
    @ConfigDocSection
    public HibernateOrmConfigPersistenceUnitDatabase database = new HibernateOrmConfigPersistenceUnitDatabase();

    /**
     * Database scripts related configuration.
     */
    @ConfigItem
    @ConfigDocSection
    public HibernateOrmConfigPersistenceUnitScripts scripts = new HibernateOrmConfigPersistenceUnitScripts();

    /**
     * Logging configuration.
     */
    @ConfigItem
    @ConfigDocSection
    public HibernateOrmConfigPersistenceUnitLog log = new HibernateOrmConfigPersistenceUnitLog();

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
    @ConfigItem
    @ConfigDocMapKey("full-property-key")
    public Map<String, String> unsupportedProperties = new HashMap<>();

    public boolean isAnyPropertySet() {
        return database.isAnyPropertySet() ||
                scripts.isAnyPropertySet() ||
                log.isAnyPropertySet() ||
                !unsupportedProperties.isEmpty();
    }

    @ConfigGroup
    public static class HibernateOrmConfigPersistenceUnitDatabase {

        /**
         * Schema generation configuration.
         */
        @ConfigItem
        public HibernateOrmConfigPersistenceUnitDatabaseGeneration generation = new HibernateOrmConfigPersistenceUnitDatabaseGeneration();

        /**
         * The default catalog to use for the database objects.
         */
        @ConfigItem
        @ConvertWith(TrimmedStringConverter.class)
        public Optional<String> defaultCatalog = Optional.empty();

        /**
         * The default schema to use for the database objects.
         */
        @ConfigItem
        @ConvertWith(TrimmedStringConverter.class)
        public Optional<String> defaultSchema = Optional.empty();

        public boolean isAnyPropertySet() {
            return generation.isAnyPropertySet()
                    || defaultCatalog.isPresent()
                    || defaultSchema.isPresent();
        }
    }

    @ConfigGroup
    public static class HibernateOrmConfigPersistenceUnitScripts {

        /**
         * Schema generation configuration.
         */
        @ConfigItem
        public HibernateOrmConfigPersistenceUnitScriptGeneration generation = new HibernateOrmConfigPersistenceUnitScriptGeneration();

        public boolean isAnyPropertySet() {
            return generation.isAnyPropertySet();
        }
    }

    @ConfigGroup
    public static class HibernateOrmConfigPersistenceUnitDatabaseGeneration {

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
        @ConfigItem(name = ConfigItem.PARENT, defaultValue = "none")
        @ConvertWith(TrimmedStringConverter.class)
        public String generation = "none";

        /**
         * If Hibernate ORM should create the schemas automatically (for databases supporting them).
         */
        @ConfigItem
        public boolean createSchemas = false;

        /**
         * Whether we should stop on the first error when applying the schema.
         */
        @ConfigItem
        public boolean haltOnError = false;

        public boolean isAnyPropertySet() {
            return !"none".equals(generation)
                    || createSchemas
                    || haltOnError;
        }
    }

    @ConfigGroup
    public static class HibernateOrmConfigPersistenceUnitScriptGeneration {

        /**
         * Select whether the database schema DDL files are generated or not.
         *
         * Accepted values: `none`, `create`, `drop-and-create`, `drop`, `update`, `validate`.
         */
        @ConfigItem(name = ConfigItem.PARENT, defaultValue = "none")
        @ConvertWith(TrimmedStringConverter.class)
        public String generation = "none";

        /**
         * Filename or URL where the database create DDL file should be generated.
         */
        @ConfigItem
        @ConvertWith(TrimmedStringConverter.class)
        public Optional<String> createTarget = Optional.empty();

        /**
         * Filename or URL where the database drop DDL file should be generated.
         */
        @ConfigItem
        @ConvertWith(TrimmedStringConverter.class)
        public Optional<String> dropTarget = Optional.empty();

        public boolean isAnyPropertySet() {
            return !"none".equals(generation)
                    || createTarget.isPresent()
                    || dropTarget.isPresent();
        }
    }

    @ConfigGroup
    public static class HibernateOrmConfigPersistenceUnitLog {

        /**
         * Show SQL logs and format them nicely.
         * <p>
         * Setting it to true is obviously not recommended in production.
         */
        @ConfigItem
        public boolean sql = false;

        /**
         * Format the SQL logs if SQL log is enabled
         */
        @ConfigItem(defaultValue = "true")
        public boolean formatSql = true;

        /**
         * Whether JDBC warnings should be collected and logged.
         */
        @ConfigItem(defaultValueDocumentation = "depends on dialect")
        public Optional<Boolean> jdbcWarnings = Optional.empty();

        /**
         * If set, Hibernate will log queries that took more than specified number of milliseconds to execute.
         */
        @ConfigItem
        public Optional<Long> queriesSlowerThanMs = Optional.empty();

        public boolean isAnyPropertySet() {
            return sql || !formatSql || jdbcWarnings.isPresent() || queriesSlowerThanMs.isPresent();
        }
    }

}
