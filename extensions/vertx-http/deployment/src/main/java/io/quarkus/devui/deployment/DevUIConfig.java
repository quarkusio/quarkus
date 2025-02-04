package io.quarkus.devui.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot
@ConfigMapping(prefix = "quarkus.dev-ui")
public interface DevUIConfig {

    /**
     * The number of history log entries to remember.
     */
    @WithDefault("50")
    int historySize();

    /**
     * Show the JsonRPC Log. Useful for extension developers
     */
    @WithDefault("false")
    boolean showJsonRpcLog();

    /**
     * More hosts allowed for Dev UI
     *
     * Comma separated list of valid URLs, e.g.: www.quarkus.io, myhost.com
     * (This can also be a regex)
     * By default localhost and 127.0.0.1 will always be allowed
     */
    Optional<List<String>> hosts();

    /**
     * CORS configuration.
     */
    Cors cors();

    @ConfigGroup
    interface Cors {

        /**
         * Enable CORS filter.
         */
        @WithDefault("true")
        boolean enabled();
    }

}
