package io.quarkus.runtime.logging;

import java.io.File;
import java.util.Optional;
import java.util.logging.Level;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
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
     * The log format
     */
    @ConfigItem(defaultValue = "%d{yyyy-MM-dd HH:mm:ss,SSS} %h %N[%i] %-5p [%c{3.}] (%t) %s%e%n")
    String format;

    /**
     * The level of logs to be written into the file.
     */
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
     * File rotation config.
     * The time interval is determined by the content of the <code>fileSuffix</code> property.
     * The size interval is determined by the content of the <code>maxFileSize</code> property.
     * If both are used, the rotating will be based on time then size.
     */
    RotationConfig rotation;

    @ConfigGroup
    public static class RotationConfig {
        /**
         * The maximum file size of the log file after which a rotation is executed.
         */
        @ConfigItem(defaultValueDocumentation = "10")
        Optional<MemorySize> maxFileSize;

        /**
         * The maximum number of backups to keep.
         */
        @ConfigItem(defaultValue = "1")
        int maxBackupIndex;

        /**
         * File handler rotation file suffix.
         * When used, the file will be rotated based on its suffix.
         *
         * Example fileSuffix: .yyyy-MM-dd
         */
        @ConfigItem
        Optional<String> fileSuffix;

        /**
         * Indicates whether to rotate log files on server initialization.
         * <p>
         * You need to either set a {@code max-file-size} or configure a {@code file-suffix} for it to work.
         */
        @ConfigItem(defaultValue = "true")
        boolean rotateOnBoot;
    }
}
