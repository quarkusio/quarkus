package io.quarkus.grpc.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@ConfigGroup
public class GrpcTransportSecurity {

    /**
     * The path to the certificate file.
     */
    @ConfigItem
    public Optional<String> certificate;

    /**
     * The path to the private key file.
     */
    @ConfigItem
    public Optional<String> key;
}
