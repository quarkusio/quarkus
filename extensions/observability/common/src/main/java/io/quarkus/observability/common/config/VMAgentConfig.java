package io.quarkus.observability.common.config;

import java.util.OptionalInt;

import io.quarkus.observability.common.ContainerConstants;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface VMAgentConfig extends ContainerConfig {
    @WithDefault(ContainerConstants.VM_AGENT)
    String imageName();

    @WithDefault("quarkus-dev-service-vm-agent")
    String label();

    OptionalInt scrapePort();
}
