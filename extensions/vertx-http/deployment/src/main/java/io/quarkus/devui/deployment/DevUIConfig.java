package io.quarkus.devui.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "dev-ui")
public class DevUIConfig {

    /**
     * The number of history log entries to remember.
     */
    @ConfigItem(defaultValue = "50")
    public int historySize;

    /**
     * Show the JsonRPC Log. Useful for extension developers
     */
    @ConfigItem(defaultValue = "false")
    public boolean showJsonRpcLog;

    /**
     * More hosts allowed for Dev UI
     *
     * Comma separated list of valid URLs, e.g.: www.quarkus.io, myhost.com
     * (This can also be a regex)
     * By default localhost and 127.0.0.1 will always be allowed
     */
    @ConfigItem
    public Optional<List<String>> hosts = Optional.empty();

    /**
     * CORS configuration.
     */
    public Cors cors = new Cors();

    @ConfigGroup
    public static class Cors {

        /**
         * Enable CORS filter.
         */
        @ConfigItem(defaultValue = "true")
        public boolean enabled = true;
    }

}
