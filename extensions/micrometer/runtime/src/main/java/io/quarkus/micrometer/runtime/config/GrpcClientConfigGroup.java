package io.quarkus.micrometer.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

/**
 * Build / static runtime config for gRPC Client.
 */
@ConfigGroup
public interface GrpcClientConfigGroup extends MicrometerConfig.CapabilityEnabled {
    /**
     * gRPC Client metrics support.
     * <p>
     * Support for gRPC client metrics will be enabled if Micrometer support is enabled,
     * the gRPC client interfaces are on the classpath
     * and either this value is true, or this value is unset and
     * {@code quarkus.micrometer.binder-enabled-default} is true.
     */
    @Override
    Optional<Boolean> enabled();
}
