package io.quarkus.devui.deployment;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot
@ConfigMapping(prefix = "quarkus.dev-mcp")
public interface DevMCPConfig {

    /**
     * Enable/disable the Dev MCP Server
     */
    @WithDefault("false")
    boolean enabled();

}
