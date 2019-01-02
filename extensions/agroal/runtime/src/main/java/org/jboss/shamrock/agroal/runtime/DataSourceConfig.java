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

package org.jboss.shamrock.agroal.runtime;

import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shamrock.runtime.ConfigGroup;

@ConfigGroup
public class DataSourceConfig {

    /**
     * The datasource driver class name
     */
    @ConfigProperty(name = "driver")
    public String driver;

    /**
     * The datasource URL
     */
    @ConfigProperty(name = "url")
    public String url;

    /**
     * The datasource username
     */
    @ConfigProperty(name = "username")
    public Optional<String> user;

    /**
     * The datasource password
     */
    @ConfigProperty(name = "password")
    public Optional<String> password;

    /**
     * The datasource pool minimum size
     */
    @ConfigProperty(name = "minSize", defaultValue = "5")
    public int minSize;

    /**
     * The datasource pool maximum size
     */
    @ConfigProperty(name = "maxSize", defaultValue = "20")
    public int maxSize;

}
