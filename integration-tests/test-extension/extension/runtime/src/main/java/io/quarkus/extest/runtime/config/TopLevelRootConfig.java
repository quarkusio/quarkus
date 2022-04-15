package io.quarkus.extest.runtime.config;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 *
 */
@ConfigRoot(name = ConfigItem.PARENT)
public class TopLevelRootConfig {
    String testProperty;
}
