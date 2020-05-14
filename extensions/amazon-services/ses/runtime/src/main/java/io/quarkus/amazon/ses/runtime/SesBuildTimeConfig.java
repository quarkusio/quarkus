package io.quarkus.amazon.ses.runtime;

import io.quarkus.amazon.common.runtime.SdkBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Amazon SES build time configuration
 */
@ConfigRoot(name = "ses", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class SesBuildTimeConfig {

    /**
     * SDK client configurations for AWS SES client
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public SdkBuildTimeConfig sdk;

    /**
     * Sync HTTP transport configuration for Amazon SES client
     */
    @ConfigItem
    public SyncHttpClientBuildTimeConfig syncClient;
}
