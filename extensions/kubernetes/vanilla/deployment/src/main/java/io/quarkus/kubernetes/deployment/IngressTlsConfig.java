package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class IngressTlsConfig {

    /**
     * If true, it will use the TLS configuration in the generated Ingress resource.
     */
    @ConfigItem
    boolean enabled;

    /**
     * The list of hosts to be included in the TLS certificate. By default, it will use the application host.
     */
    @ConfigItem
    Optional<List<String>> hosts;
}
