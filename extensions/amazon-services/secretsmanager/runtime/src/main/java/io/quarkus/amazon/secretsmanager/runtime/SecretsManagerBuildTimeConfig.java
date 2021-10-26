package io.quarkus.amazon.secretsmanager.runtime;

import io.quarkus.amazon.common.runtime.SdkBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Amazon Secrets Manager build time configuration
 */
@ConfigRoot(name = "secretsmanager", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class SecretsManagerBuildTimeConfig {

    /**
     * SDK client configurations for AWS Secrets Manager client
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public SdkBuildTimeConfig sdk;

    /**
     * Sync HTTP transport configuration for Amazon Secrets Manager client
     */
    @ConfigItem
    public SyncHttpClientBuildTimeConfig syncClient;
}
