package io.quarkus.liquibase.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * The liquibase configuration
 */
@ConfigRoot(name = "liquibase-mongodb", phase = ConfigPhase.RUN_TIME)
public class LiquibaseMongodbConfig {

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

    public Map<String, String> changeLogParameters = null;

    /**
     * The list of contexts
     */
    public Optional<List<String>> contexts = null;

    /**
     * The list of labels
     */
    public Optional<List<String>> labels = null;

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
