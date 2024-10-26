
package io.quarkus.kubernetes.deployment;

import java.util.Map;

import io.dekorate.kubernetes.config.HostAlias;
import io.dekorate.kubernetes.config.HostAliasBuilder;

public class HostAliasConverter {

    public static HostAlias convert(Map.Entry<String, HostAliasConfig> e) {
        return convert(e.getValue()).withIp(e.getKey()).build();
    }

    public static HostAliasBuilder convert(HostAliasConfig hostAlias) {
        HostAliasBuilder b = new HostAliasBuilder();
        hostAlias.hostnames().ifPresent(h -> b.withHostnames(String.join(",", h)));

        return b;
    }

}
