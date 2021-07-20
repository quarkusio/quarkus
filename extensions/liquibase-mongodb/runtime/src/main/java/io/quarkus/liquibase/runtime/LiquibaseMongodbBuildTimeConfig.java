package io.quarkus.liquibase.runtime;

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
    public String changeLog = "db/changeLog.xml";
}
