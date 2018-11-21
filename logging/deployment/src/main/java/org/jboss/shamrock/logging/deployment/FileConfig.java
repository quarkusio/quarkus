package org.jboss.shamrock.logging.deployment;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shamrock.runtime.ConfiguredType;

@ConfiguredType
public class FileConfig {

    /**
     * If file logging should be enabled
     */
    @ConfigProperty(name = "enable", defaultValue = "true")
    boolean enable;

    /**
     * The log format
     */
    @ConfigProperty(name = "format", defaultValue = "%d{yyyy-MM-dd HH:mm:ss,SSS} %h %N[%i] %-5p [%c{1.}] (%t) %s%e%n")
    String format;

    /**
     * The file log level
     */
    @ConfigProperty(name = "level", defaultValue = "ALL")
    String level;

    /**
     * The file logging log level
     */
    @ConfigProperty(name = "path", defaultValue = "shamrock.log")
    String path;

}
