package io.quarkus.deployment.builditem;

import java.util.Collections;
import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.logging.InheritableLevel;

/**
 * A build item containing the default minimum log levels for specific log categories.
 * Minimum log levels ({@link io.quarkus.runtime.logging.InheritableLevel}).
 */
public final class LogCategoryMinLevelDefaultsBuildItem extends SimpleBuildItem {

    public final Map<String, InheritableLevel> content;

    public LogCategoryMinLevelDefaultsBuildItem(Map<String, InheritableLevel> content) {
        this.content = Collections.unmodifiableMap(content);
    }

}
