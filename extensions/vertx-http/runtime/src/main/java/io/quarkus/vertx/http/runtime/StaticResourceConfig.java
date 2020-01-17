package io.quarkus.vertx.http.runtime;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class StaticResourceConfig {

    /**
     * General static resource configuration
     */
    @ConfigDocMapKey("static-resource-name")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, StaticResourceGeneralConfig> config;

    @ConfigGroup
    public static class StaticResourceGeneralConfig {
        /**
         * The endpoint from which the static resources will be served.
         */
        @ConfigItem
        public String endpoint;

        /**
         * Enable caching of static resources.
         */
        @ConfigItem(defaultValue = "true")
        public boolean enableCache;

        /**
         * The maximum cache size.
         */
        @ConfigItem(defaultValue = "10000")
        public OptionalInt cacheSize;

        /**
         * The default landing page of static resource.
         */
        @ConfigItem(defaultValue = "index.html")
        public Optional<String> index;

        /**
         * Allow directory listing.
         */
        @ConfigItem
        public boolean allowDirectoryListing;

        /**
         * The path to a directory from which the static resources will be served.
         */
        @ConfigItem
        public String path;
    }
}
