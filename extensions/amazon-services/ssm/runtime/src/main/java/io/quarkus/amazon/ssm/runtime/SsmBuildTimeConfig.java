package io.quarkus.amazon.ssm.runtime;

import io.quarkus.amazon.common.runtime.SdkBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Amazon SSM build time configuration
 */
@ConfigRoot(name = "ssm", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class SsmBuildTimeConfig {

    /**
     * SDK client configurations for AWS SSM client
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public SdkBuildTimeConfig sdk;

    /**
     * Sync HTTP transport configuration for Amazon SSM client
     */
    @ConfigItem
    public SyncHttpClientBuildTimeConfig syncClient;
}
