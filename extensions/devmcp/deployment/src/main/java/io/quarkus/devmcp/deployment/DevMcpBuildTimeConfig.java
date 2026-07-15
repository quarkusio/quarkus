package io.quarkus.devmcp.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot
@ConfigMapping(prefix = "quarkus.dev-mcp")
public interface DevMcpBuildTimeConfig {

    /**
     * Additional hosts allowed for Dev MCP access.
     *
     * Comma separated list of valid URLs, e.g.: www.quarkus.io, myhost.com
     * (This can also be a regex, e.g.: {@code ^([A-Za-z0-9-]+).apps.myhost.com})
     * By default localhost and 127.0.0.1 will always be allowed.
     */
    Optional<List<String>> hosts();

    /**
     * CORS configuration for the Dev MCP endpoint.
     */
    Cors cors();

    @ConfigGroup
    interface Cors {

        /**
         * Enable CORS filter for the Dev MCP endpoint.
         */
        @WithDefault("true")
        boolean enabled();
    }
}
