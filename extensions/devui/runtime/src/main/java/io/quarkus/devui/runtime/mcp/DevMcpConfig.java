package io.quarkus.devui.runtime.mcp;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.dev-mcp")
public interface DevMcpConfig {

    /**
     * Enable/Disable the Dev MCP server.
     * This overrides the value in ~/.quarkus/dev-mcp.properties.
     */
    Optional<Boolean> enabled();
}
