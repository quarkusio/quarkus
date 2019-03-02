package io.quarkus.hibernate.orm;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@ConfigRoot
public class HibernateOrmConfig {
    /**
     * The hibernate ORM dialect class name
     */
    // TODO should it be dialects
    //TODO should it be shortcuts like "postgresql" "h2" etc
    @ConfigItem
    public Optional<String> dialect;

    /**
     * The storage engine used by the dialect if it supports several storage engines.
     * <p>
     * This is the case of MariaDB.
     */
    @ConfigItem(name = "dialect.storage-engine")
    public Optional<String> dialectStorageEngine;

    /**
     * To populate the database tables with data before the application loads,
     * specify the location of a load script.
     * The location specified in this property is relative to the root of the persistence unit.
     */
    @ConfigItem
    public Optional<String> sqlLoadScript;

    /**
     * Query related configuration.
     */
    @ConfigItem
    public HibernateOrmConfigQuery query;

    /**
     * Schema related configuration.
     */
    @ConfigItem
    public HibernateOrmConfigSchema schema;

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
     * Statistics configuration.
     */
    @ConfigItem
    public HibernateOrmConfigStatistics statistics;

    public boolean isAnyPropertySet() {
        return dialect.isPresent() || dialectStorageEngine.isPresent() || sqlLoadScript.isPresent() ||
                query.isAnyPropertySet() ||
                schema.isAnyPropertySet() ||
                jdbc.isAnyPropertySet() ||
                log.isAnyPropertySet() ||
                statistics.isAnyPropertySet();
    }

    @ConfigGroup
    public static class HibernateOrmConfigQuery {

        /**
         * The max size of the query plan cache.
         */
        @ConfigItem
        public Optional<String> queryPlanCacheMaxSize;

        /**
         * The size of a batch when using batch loading.
         * <p>
         * -1 means batch loading is disabled.
         */
        @ConfigItem(defaultValue = "-1")
        public int batchFetchSize;

        /**
         * The default ordering of nulls specific in the ORDER BY clause.
         * <p>
         * Valid values are: none, first, last.
         */
        @ConfigItem
        public Optional<String> defaultNullOrdering;

        public boolean isAnyPropertySet() {
            return queryPlanCacheMaxSize.isPresent() || batchFetchSize > 0 || defaultNullOrdering.isPresent();
        }
    }

    @ConfigGroup
    public static class HibernateOrmConfigSchema {

        /**
         * Control how schema generation is happening in Hibernate ORM.
         * <p>
         * Same as JPA's javax.persistence.schema-generation.database.action.
         */
        @ConfigItem
        public Optional<String> generation;

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
         * Whether we should stop schema application at the first error or continue.
         */
        @ConfigItem(defaultValue = "false")
        public boolean haltOnError;

        /**
         * The charset of the database.
         */
        @ConfigItem
        public Optional<String> charset;

        public boolean isAnyPropertySet() {
            return generation.isPresent() || defaultCatalog.isPresent() || defaultSchema.isPresent() || haltOnError
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
         * Whether we log all the SQL queries executed.
         * <p>
         * Setting it to true is obviously not recommended in production.
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
    public static class HibernateOrmConfigStatistics {

        /**
         * Whether statistics are collected.
         */
        @ConfigItem(defaultValue = "false")
        public boolean enabled;

        public boolean isAnyPropertySet() {
            return enabled;
        }
    }
}
