package io.quarkus.amazon.dynamodb.runtime;

import io.quarkus.amazon.common.runtime.SdkBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Amazon DynamoDb build time configuration
 */
@ConfigRoot(name = "dynamodb", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class DynamodbBuildTimeConfig {

    /**
     * SDK client configurations for AWS Dynamodb client
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public SdkBuildTimeConfig sdk;

    /**
     * Sync HTTP transport configuration for Amazon Dynamodb client
     */
    @ConfigItem
    public SyncHttpClientBuildTimeConfig syncClient;
}
