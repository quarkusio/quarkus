package io.quarkus.liquibase.mongodb.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * The liquibase configuration
 */
@ConfigRoot(name = "liquibase-mongodb", phase = ConfigPhase.BUILD_TIME)
public class LiquibaseMongodbBuildTimeConfig {

    /**
     * The change log file
     */
    @ConfigItem(defaultValue = "db/changeLog.xml")
    public String changeLog;

    /**
     * The search path for DirectoryResourceAccessor
     */
    @ConfigItem
    public Optional<List<String>> searchPath;
}
