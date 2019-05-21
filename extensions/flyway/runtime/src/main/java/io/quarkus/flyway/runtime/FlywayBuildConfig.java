/*
 * Copyright 2019 Red Hat, Inc.
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

package io.quarkus.flyway.runtime;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "flyway", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public final class FlywayBuildConfig {
    /**
     * Comma-separated list of locations to scan recursively for migrations. The location type is determined by its prefix.
     * Unprefixed locations or locations starting with classpath: point to a package on the classpath and may contain both SQL
     * and Java-based migrations.
     * Locations starting with filesystem: point to a directory on the filesystem, may only contain SQL migrations and are only
     * scanned recursively down non-hidden directories.
     */
    @ConfigItem
    public List<String> locations;
}
