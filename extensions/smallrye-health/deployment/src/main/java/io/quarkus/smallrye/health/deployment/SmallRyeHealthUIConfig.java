package io.quarkus.smallrye.health.deployment;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class SmallRyeHealthUIConfig {
    /**
     * The path where Health UI is available.
     * The value `/` is not allowed as it blocks the application from serving anything else.
     * By default, this value will be resolved as a path relative to `${quarkus.http.non-application-root-path}`.
     */
    @ConfigItem(defaultValue = "health-ui")
    String rootPath;

    /**
     * Always include the UI. By default this will only be included in dev and test.
     * Setting this to true will also include the UI in Prod
     */
    @ConfigItem(defaultValue = "false")
    boolean alwaysInclude;

}
