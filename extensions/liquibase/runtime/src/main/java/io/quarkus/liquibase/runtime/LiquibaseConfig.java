package io.quarkus.liquibase.runtime;

import static io.quarkus.liquibase.runtime.LiquibaseDataSourceBuildTimeConfig.DEFAULT_CHANGE_LOG;
import static io.quarkus.liquibase.runtime.LiquibaseDataSourceRuntimeConfig.DEFAULT_LOCK_TABLE;
import static io.quarkus.liquibase.runtime.LiquibaseDataSourceRuntimeConfig.DEFAULT_LOG_TABLE;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The liquibase configuration
 */
public class LiquibaseConfig {

    /**
     * The change log file
     */
    public String changeLog = DEFAULT_CHANGE_LOG;

    /**
     * The migrate at start flag
     */
    public boolean migrateAtStart = false;

    /**
     * The validate on update flag
     */
    public boolean validateOnMigrate = true;

    /**
     * The clean at start flag
     */
    public boolean cleanAtStart = false;

    /**
     * The list of contexts
     */
    public List<String> contexts = null;

    /**
     * The list of labels
     */
    public List<String> labels = null;

    public Map<String, String> changeLogParameters = null;

    /**
     * The database change log lock table name
     */
    public String databaseChangeLogLockTableName = DEFAULT_LOCK_TABLE;

    /**
     * The database change log table name
     */
    public String databaseChangeLogTableName = DEFAULT_LOG_TABLE;

    /**
     * The default catalog name
     */
    public Optional<String> defaultCatalogName = Optional.empty();

    /**
     * The default schema name
     */
    public Optional<String> defaultSchemaName = Optional.empty();

    /**
     * The liquibase tables catalog name
     */
    public Optional<String> liquibaseCatalogName = Optional.empty();

    /**
     * The liquibase tables schema name
     */
    public Optional<String> liquibaseSchemaName = Optional.empty();

    /**
     * The liquibase tables tablespace name
     */
    public Optional<String> liquibaseTablespaceName = Optional.empty();

}
