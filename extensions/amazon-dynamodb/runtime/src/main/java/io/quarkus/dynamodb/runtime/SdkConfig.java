package io.quarkus.dynamodb.runtime;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * AWS SDK specific configurations
 */
@ConfigGroup
public class SdkConfig {
    /**
     * Configure the endpoint URI with which the SDK should communicate.
     *
     * <p>
     * If not specified, an appropriate endpoint to be used for DynamoDB service and region.
     */
    @ConfigItem
    public Optional<URI> endpointOverride;

    /**
     * Configure the amount of time to allow the client to complete the execution of an API call.
     *
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
     * Configure the amount of time to wait for the http request to complete before giving up and timing out.
     *
     * <p>
     * This value should always be positive, if present.
     * 
     * @see software.amazon.awssdk.core.client.config.ClientOverrideConfiguration#apiCallAttemptTimeout()
     */
    @ConfigItem
    public Optional<Duration> apiCallAttemptTimeout;

    /**
     * List of execution interceptors that will have access to read and modify the request and response objects as they are
     * processed by the AWS SDK.
     *
     * <p>
     * The list should consists of class names which implements
     * {@code software.amazon.awssdk.core.interceptor.ExecutionInterceptor} interface.
     * 
     * @see software.amazon.awssdk.core.interceptor.ExecutionInterceptor
     */
    @ConfigItem
    public List<Class<?>> interceptors;

    public boolean isClientOverrideConfig() {
        return apiCallTimeout.isPresent() || apiCallAttemptTimeout.isPresent()
                || (interceptors != null && !interceptors.isEmpty());
    }
}
