package io.quarkus.micrometer.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

/**
 * Build / static runtime config for gRPC Server.
 */
@ConfigGroup
public interface GrpcServerConfigGroup extends MicrometerConfig.CapabilityEnabled {
    /**
     * gRPC Server metrics support.
     * <p>
     * Support for gRPC server metrics will be enabled if Micrometer support is enabled,
     * the gRPC server interfaces are on the classpath
     * and either this value is true, or this value is unset and
     * {@code quarkus.micrometer.binder-enabled-default} is true.
     */
    @Override
    Optional<Boolean> enabled();
}
