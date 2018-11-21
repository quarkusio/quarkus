package org.jboss.shamrock.logging.deployment;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shamrock.runtime.ConfiguredType;

@ConfiguredType
public class CategoryConfig {

    /**
     * The minimum level that this category can be set to
     */
    @ConfigProperty(name = "min-level", defaultValue = "inherit")
    String minLevel;

    /**
     * The log level level for this category
     */
    @ConfigProperty(name = "level", defaultValue = "inherit")
    String level;
}
