package io.quarkus.grpc.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class GrpcTransportSecurity {

    /**
     * The path to the certificate file.
     */
    @ConfigItem
    Optional<String> certificate;

    /**
     * The path to the private key file.
     */
    @ConfigItem
    Optional<String> key;
}
