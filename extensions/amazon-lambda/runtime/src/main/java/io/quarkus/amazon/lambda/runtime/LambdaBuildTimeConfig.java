package io.quarkus.amazon.lambda.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 *
 */
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class LambdaBuildTimeConfig {

    /**
     * If true, this will enable the aws event poll loop within a Quarkus test run. This loop normally only runs in native
     * image. This option is strictly for testing purposes.
     *
     */
    @ConfigItem
    public boolean enablePollingJvmMode;
}
