package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.Optional;

public interface HostAliasConfig {
    /**
     * The ip address.
     */
    Optional<String> ip();

    /**
     * The hostnames to resolve to the ip.
     */
    Optional<List<String>> hostnames();
}
