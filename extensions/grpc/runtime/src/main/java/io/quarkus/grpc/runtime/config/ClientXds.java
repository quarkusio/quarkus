package io.quarkus.grpc.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Client XDS config
 * * <a href="https://github.com/grpc/grpc-java/tree/master/examples/example-xds">XDS usage</a>
 */
@ConfigGroup
public class ClientXds extends Xds {
    /**
     * Optional explicit target.
     */
    @ConfigItem
    public Optional<String> target;
}
