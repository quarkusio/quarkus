/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.runtime.logging;

import java.io.File;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.logging.Level;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class FileConfig {

    /**
     * If file logging should be enabled
     */
    @ConfigItem(defaultValue = "false")
    boolean enable;

    /**
     * The log format
     */
    @ConfigItem(defaultValue = "%d{yyyy-MM-dd HH:mm:ss,SSS} %h %N[%i] %-5p [%c{3.}] (%t) %s%e%n")
    String format;

    /**
     * The file log level
     */
    @ConfigItem(defaultValue = "ALL")
    Level level;

    /**
     * The file logging log level
     */
    @ConfigItem(defaultValue = "quarkus.log")
    File path;

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
        OptionalLong maxFileSize;

        /**
         * The maximum number of backups to keep.
         */
        @ConfigItem(defaultValue = "1")
        int maxBackupIndex;

        /**
         * File handler rotation file suffix.
         *
         * Example fileSuffix: .yyyy-MM-dd
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
