package io.quarkus.dynamodb.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "dynamodb", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class DynamodbBuildTimeConfig {

    /**
     * SDK client configurations
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public SdkBuildTimeConfig sdk;

    /**
     * Sync client transport configuration
     */
    @ConfigItem(name = "sync-client")
    public SyncHttpClientBuildTimeConfig syncClient;
}
