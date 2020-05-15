package io.quarkus.amazon.sns.runtime;

import io.quarkus.amazon.common.runtime.SdkBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Amazon SNS build time configuration
 */
@ConfigRoot(name = "sns", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class SnsBuildTimeConfig {

    /**
     * SDK client configurations for AWS SNS client
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public SdkBuildTimeConfig sdk;

    /**
     * Sync HTTP transport configuration for Amazon SNS client
     */
    @ConfigItem
    public SyncHttpClientBuildTimeConfig syncClient;
}
