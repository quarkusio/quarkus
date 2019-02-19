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

package io.quarkus.agroal.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DataSourceRuntimeConfig {

    /**
     * The datasource URL
     */
    @ConfigItem
    public Optional<String> url;

    /**
     * The datasource username
     */
    @ConfigItem
    public Optional<String> username;

    /**
     * The datasource password
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * The initial size of the pool
     */
    @ConfigItem
    public Optional<Integer> initialSize;

    /**
     * The datasource pool minimum size
     */
    @ConfigItem(defaultValue = "5")
    public int minSize;

    /**
     * The datasource pool maximum size
     */
    @ConfigItem(defaultValue = "20")
    public int maxSize;

    /**
     * The interval at which we validate idle connections in the background
     */
    @ConfigItem(defaultValue = "PT2M")
    public Optional<Duration> backgroundValidationInterval;

    /**
     * The timeout before cancelling the acquisition of a new connection
     */
    @ConfigItem(defaultValue = "PT5S")
    public Optional<Duration> acquisitionTimeout;

    /**
     * The interval at which we check for connection leaks.
     */
    @ConfigItem
    public Optional<Duration> leakDetectionInterval;

    /**
     * The interval at which we try to remove idle connections.
     */
    @ConfigItem(defaultValue = "PT5M")
    public Optional<Duration> idleRemovalInterval;

}
