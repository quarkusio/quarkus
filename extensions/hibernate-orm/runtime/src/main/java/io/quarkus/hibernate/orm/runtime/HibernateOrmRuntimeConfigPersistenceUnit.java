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
     * Logging configuration.
     */
    @ConfigItem
    @ConfigDocSection
    public HibernateOrmConfigPersistenceUnitLog log = new HibernateOrmConfigPersistenceUnitLog();

    public boolean isAnyPropertySet() {
        return database.isAnyPropertySet() ||
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
    public static class HibernateOrmConfigPersistenceUnitDatabaseGeneration {

        /**
         * Select whether the database schema is generated or not.
         *
         * `drop-and-create` is awesome in development mode.
         *
         * Accepted values: `none`, `create`, `drop-and-create`, `drop`, `update`.
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

        public boolean isAnyPropertySet() {
            return sql || !formatSql || jdbcWarnings.isPresent();
        }
    }

}
