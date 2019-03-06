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

package io.quarkus.undertow.runtime;

import java.util.OptionalInt;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.ssl.ServerSslConfig;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class HttpConfig {

    /**
     * The HTTP port
     */
    @ConfigItem(defaultValue = "8080")
    public int port;

    /**
     * The HTTPS port
     */
    @ConfigItem(defaultValue = "8443")
    public int sslPort;

    /**
     * The HTTP port used to run tests
     */
    @ConfigItem(defaultValue = "8081")
    public int testPort;

    /**
     * The HTTPS port used to run tests
     */
    @ConfigItem(defaultValue = "8444")
    public int testSslPort;
    /**
     * The HTTP host
     */
    @ConfigItem(defaultValue = "localhost")
    public String host;

    /**
     * The number of worker threads used for blocking tasks, this will be automatically set to a reasonable value
     * based on the number of CPU core if it is not provided
     */
    @ConfigItem
    public OptionalInt workerThreads;

    /**
     * The number if IO threads used to perform IO. This will be automatically set to a reasonable value based on
     * the number of CPU cores if it is not provided
     */
    @ConfigItem
    public OptionalInt ioThreads;

    /**
     * The SSL config
     */
    public ServerSslConfig ssl;

    public int determinePort(LaunchMode launchMode) {
        return launchMode == LaunchMode.TEST ? testPort : port;
    }

    public int determineSslPort(LaunchMode launchMode) {
        return launchMode == LaunchMode.TEST ? testSslPort : sslPort;
    }

}
