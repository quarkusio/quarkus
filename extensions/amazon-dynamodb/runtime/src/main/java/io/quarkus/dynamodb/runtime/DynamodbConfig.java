package io.quarkus.dynamodb.runtime;

import java.net.URI;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import software.amazon.awssdk.regions.Region;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class DynamodbConfig {

    /**
     * Overrides region provider chain with static value of region with which the DynamoDB client should communicate (e.g.
     * eu-west-1, eu-central-1, us-east-1, etc.)
     */
    @ConfigItem
    public Optional<Region> region;

    /**
     * Enable endpoint discovery
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableEndpointDiscovery;

    /**
     * Overrides endpoint URI
     */
    @ConfigItem
    public Optional<URI> endpointOverride;

    /**
     * Defines credentials provider used
     */
    @ConfigItem
    public AwsCredentialsProviderConfig credentials;

    /**
     * Apache HTTP client transport configuration
     */
    @ConfigItem
    public AwsApacheHttpClientConfig syncClient;

    /**
     * Netty HTTP client transport configuration
     */
    @ConfigItem
    public AwsNettyNioAsyncHttpClientConfig asyncClient;

}
