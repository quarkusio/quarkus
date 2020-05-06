package io.quarkus.amazon.common.runtime;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * AWS SDK specific configurations
 */
@ConfigGroup
public class SdkConfig {
    /**
     * The endpoint URI with which the SDK should communicate.
     * <p>
     * If not specified, an appropriate endpoint to be used for DynamoDB service and region.
     */
    @ConfigItem
    public Optional<URI> endpointOverride;

    /**
     * The amount of time to allow the client to complete the execution of an API call.
     * <p>
     * This timeout covers the entire client execution except for marshalling. This includes request handler execution, all HTTP
     * requests including retries, unmarshalling, etc.
     * <p>
     * This value should always be positive, if present.
     *
     * @see software.amazon.awssdk.core.client.config.ClientOverrideConfiguration#apiCallTimeout()
     **/
    @ConfigItem
    public Optional<Duration> apiCallTimeout;

    /**
     * The amount of time to wait for the HTTP request to complete before giving up and timing out.
     * <p>
     * This value should always be positive, if present.
     *
     * @see software.amazon.awssdk.core.client.config.ClientOverrideConfiguration#apiCallAttemptTimeout()
     */
    @ConfigItem
    public Optional<Duration> apiCallAttemptTimeout;
}
