package org.jboss.shamrock.logging.deployment;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shamrock.runtime.ConfiguredType;

@ConfiguredType
public class ConsoleConfig {

    /**
     * If console logging should be enabled
     */
    @ConfigProperty(name = "enable", defaultValue = "true")
    boolean enable;

    /**
     * The log format
     */
    @ConfigProperty(name = "format", defaultValue = "%d{yyyy-MM-dd HH:mm:ss,SSS} %h %N[%i] %-5p [%c{1.}] (%t) %s%e%n")
    String format;

    /**
     * The console log level
     */
    @ConfigProperty(name = "level", defaultValue = "INFO")
    String level;

    /**
     * If the console logging should be in color
     */
    @ConfigProperty(name = "color", defaultValue = "true")
    boolean color;

}
