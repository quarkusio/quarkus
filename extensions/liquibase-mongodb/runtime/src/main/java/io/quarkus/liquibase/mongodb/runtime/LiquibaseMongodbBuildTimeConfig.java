package io.quarkus.liquibase.mongodb.runtime;

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
     * Flag to enable / disable the generation of the init task Kubernetes resources.
     * This property is only relevant if the Quarkus Kubernetes/OpenShift extensions are present.
     *
     * The default value is `quarkus.liquibase-mongodb.enabled`.
     */
    @ConfigItem(defaultValue = "${quarkus.liquibase-mongodb.enabled:true}")
    public boolean generateInitTask;

    /**
     * Flag to enable / disable the migration using the generated init task Kubernetes resources.
     * This property is only relevant if the Quarkus Kubernetes/OpenShift extensions are present.
     *
     * The default value is `quarkus.liquibase-mongodb.migrate-at-start`.
     */
    @ConfigItem(defaultValue = "${quarkus.liquibase-mongodb.migrate-at-start:false}")
    public boolean migrateWithInitTask;
}
