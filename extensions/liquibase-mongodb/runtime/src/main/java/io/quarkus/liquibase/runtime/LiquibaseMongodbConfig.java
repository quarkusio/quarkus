package io.quarkus.liquibase.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
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
    @ConfigItem
    public boolean migrateAtStart;

    /**
     * The validate on update flag
     */
    @ConfigItem(defaultValue = "true")
    public boolean validateOnMigrate;

    /**
     * The clean at start flag
     */
    @ConfigItem
    public boolean cleanAtStart;

    /**
     * The parameters to be passed to the changelog.
     * Defined as key value pairs.
     */
    @ConfigItem
    public Map<String, String> changeLogParameters = new HashMap<>();;

    /**
     * The list of contexts
     */
    @ConfigItem
    public Optional<List<String>> contexts = Optional.empty();

    /**
     * The list of labels
     */
    @ConfigItem
    public Optional<List<String>> labels = Optional.empty();

    /**
     * The default catalog name
     */
    @ConfigItem
    public Optional<String> defaultCatalogName = Optional.empty();

    /**
     * The default schema name
     */
    @ConfigItem
    public Optional<String> defaultSchemaName = Optional.empty();

    /**
     * The liquibase tables catalog name
     */
    @ConfigItem
    public Optional<String> liquibaseCatalogName = Optional.empty();

    /**
     * The liquibase tables schema name
     */
    @ConfigItem
    public Optional<String> liquibaseSchemaName = Optional.empty();

    /**
     * The liquibase tables tablespace name
     */
    @ConfigItem
    public Optional<String> liquibaseTablespaceName = Optional.empty();
}
