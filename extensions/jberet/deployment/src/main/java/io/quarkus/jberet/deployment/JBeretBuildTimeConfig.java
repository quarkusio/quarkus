package io.quarkus.jberet.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "jberet", phase = ConfigPhase.BUILD_TIME)
public class JBeretBuildTimeConfig {

    /**
     * Thread pool size for the JobOperator.
     */
    @ConfigItem(name = "maximum.pool.size", defaultValue = "10")
    public Integer maximumPoolSize;

}
