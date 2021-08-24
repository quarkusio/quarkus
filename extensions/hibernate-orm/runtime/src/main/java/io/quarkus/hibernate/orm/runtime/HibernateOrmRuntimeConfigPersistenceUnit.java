package io.quarkus.hibernate.orm.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class HibernateOrmRuntimeConfigPersistenceUnit {

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

    public boolean isAnyPropertySet() {
        return database.isAnyPropertySet() ||
                scripts.isAnyPropertySet() ||
                log.isAnyPropertySet();
    }

    @ConfigGroup
    public static class HibernateOrmConfigPersistenceUnitDatabase {

        /**
         * Schema generation configuration.
         */
        @ConfigItem
        public HibernateOrmConfigPersistenceUnitDatabaseGeneration generation = new HibernateOrmConfigPersistenceUnitDatabaseGeneration();

        public boolean isAnyPropertySet() {
            return generation.isAnyPropertySet();
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
         * Accepted values: `none`, `create`, `drop-and-create`, `drop`, `update`, `validate`.
         */
        @ConfigItem(name = ConfigItem.PARENT, defaultValue = "none")
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
        public String generation = "none";

        /**
         * Filename or URL where the database create DDL file should be generated.
         */
        @ConfigItem
        public Optional<String> createTarget = Optional.empty();

        /**
         * Filename or URL where the database drop DDL file should be generated.
         */
        @ConfigItem
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
