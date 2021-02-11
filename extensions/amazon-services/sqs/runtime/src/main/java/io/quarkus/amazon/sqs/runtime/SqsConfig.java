package io.quarkus.amazon.sqs.runtime;

import io.quarkus.amazon.common.runtime.AwsConfig;
import io.quarkus.amazon.common.runtime.NettyHttpClientConfig;
import io.quarkus.amazon.common.runtime.SdkConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class SqsConfig {
    /**
     * AWS SDK client configurations
     */
    @ConfigItem(name = ConfigItem.PARENT)
    @ConfigDocSection
    public SdkConfig sdk;

    /**
     * AWS services configurations
     */
    @ConfigItem
    @ConfigDocSection
    public AwsConfig aws;

    /**
     * Sync HTTP transport configurations
     */
    @ConfigItem
    @ConfigDocSection
    public SyncHttpClientConfig syncClient;

    /**
     * Netty HTTP transport configurations
     */
    @ConfigItem
    @ConfigDocSection
    public NettyHttpClientConfig asyncClient;
}
