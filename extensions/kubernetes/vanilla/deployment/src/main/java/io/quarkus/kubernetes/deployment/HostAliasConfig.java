
package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class HostAliasConfig {

    /**
     * The ip address
     *
     * @return The ip.
     */
    @ConfigItem
    Optional<String> ip;

    /**
     * The hostnames to resolve to the ip
     *
     * @return The path.
     */
    @ConfigItem
    Optional<List<String>> hostnames;
}
