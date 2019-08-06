package io.quarkus.runtime.logging;

import java.io.File;
import java.util.Optional;
import java.util.logging.Level;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.DefaultConverter;
import io.quarkus.runtime.configuration.MemorySize;

@ConfigGroup
public class FileConfig {

    /**
     * Default file name where logs should be stored.
     */
    public static final String DEFAULT_LOG_FILE_NAME = "quarkus.log";

    /**
     * If file logging should be enabled
     */
    @ConfigItem
    boolean enable;

    /**
     * The format pattern to use for logging to a file. see <<format_string>>
     */
    @ConfigItem(defaultValue = "%d{yyyy-MM-dd HH:mm:ss,SSS} %h %N[%i] %-5p [%c{3.}] (%t) %s%e%n")
    String format;

    /**
     * The minimum log level to be written into the file.
     */
    @DefaultConverter
    @ConfigItem(defaultValue = "ALL")
    Level level;

    /**
     * The name of the file in which logs will be written.
     */
    @ConfigItem(defaultValue = DEFAULT_LOG_FILE_NAME)
    File path;

    /**
     * File async logging config
     */
    AsyncConfig async;

    /**
     * File rotation config
     */
    RotationConfig rotation;

    @ConfigGroup
    public static class RotationConfig {
        /**
         * The maximum file size of the log file after which a rotation is executed.
         */
        @ConfigItem
        Optional<MemorySize> maxFileSize;

        /**
         * The maximum number of backups to keep.
         */
        @ConfigItem(defaultValue = "1")
        int maxBackupIndex;

        /**
         * Rotating log file suffix. The format of suffix value has to be understood by `java.text.SimpleDateFormat`.
         * Example fileSuffix: `.yyyy-MM-dd`
         */
        @ConfigItem
        Optional<String> fileSuffix;

        /**
         * Indicates whether to rotate log files on server initialization.
         */
        @ConfigItem(defaultValue = "true")
        boolean rotateOnBoot;
    }
}
