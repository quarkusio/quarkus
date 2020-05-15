package io.quarkus.amazon.sqs.runtime;

import io.quarkus.amazon.common.runtime.SdkBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Amazon SQS build time configuration
 */
@ConfigRoot(name = "sqs", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class SqsBuildTimeConfig {

    /**
     * SDK client configurations for AWS SQS client
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public SdkBuildTimeConfig sdk;

    /**
     * Sync HTTP transport configuration for Amazon SQS client
     */
    @ConfigItem
    public SyncHttpClientBuildTimeConfig syncClient;
}
