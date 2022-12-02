package io.quarkus.vertx.http.deployment.devmode.console;

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
