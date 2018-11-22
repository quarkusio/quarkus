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

package org.jboss.shamrock.logging.deployment;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shamrock.runtime.ConfigGroup;

@ConfigGroup
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
