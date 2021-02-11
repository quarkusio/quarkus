package io.quarkus.logging.manager.deployment;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class LoggingManagerConfig {

    /**
     * The base path, defaults to /logging-manager
     */
    @ConfigItem(defaultValue = "/logging-manager")
    String basePath;

    /**
     * UI configuration
     */
    @ConfigItem
    @ConfigDocSection
    LoggingManagerUIConfig ui;
}
