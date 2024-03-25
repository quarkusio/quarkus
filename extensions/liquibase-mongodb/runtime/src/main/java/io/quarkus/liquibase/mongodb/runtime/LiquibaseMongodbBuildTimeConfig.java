package io.quarkus.liquibase.mongodb.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

import java.util.List;

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
    public List<String> searchPath;
}
