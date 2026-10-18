
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

    public static io.fabric8.kubernetes.api.model.HostAlias toKubeHostAlias(Map.Entry<String, HostAliasConfig> e) {
        final var b = new io.fabric8.kubernetes.api.model.HostAliasBuilder();
        b.withIp(e.getKey());
        e.getValue().hostnames().ifPresent(b::withHostnames);
        return b.build();
    }
}
