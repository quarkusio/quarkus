package io.quarkus.amazon.iam.runtime;

import io.quarkus.amazon.common.runtime.SdkBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Amazon IAM build time configuration
 */
@ConfigRoot(name = "iam", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class IamBuildTimeConfig {

    /**
     * SDK client configurations for AWS IAM client
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public SdkBuildTimeConfig sdk;

    /**
     * Sync HTTP transport configuration for Amazon IAM client
     */
    @ConfigItem
    public SyncHttpClientBuildTimeConfig syncClient;
}
