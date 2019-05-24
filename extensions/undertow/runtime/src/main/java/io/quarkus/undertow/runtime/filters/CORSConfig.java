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
package io.quarkus.undertow.runtime.filters;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class CORSConfig {

    /**
     * Origins allowed for CORS
     *
     * Comma separated list of valid URLs. ex: http://www.quarkus.io,http://localhost:3000
     * The filter allows any origin if this is not set.
     *
     * default: returns any requested origin as valid
     */
    @ConfigItem
    public Optional<String> origins;
    /**
     * HTTP methods allowed for CORS
     *
     * Comma separated list of valid methods. ex: GET,PUT,POST
     * The filter allows any method if this is not set.
     *
     * default: returns any requested method as valid
     */
    @ConfigItem
    public Optional<String> methods;
    /**
     * HTTP headers allowed for CORS
     *
     * Comma separated list of valid headers. ex: X-Custom,Content-Disposition
     * The filter allows any header if this is not set.
     *
     * default: returns any requested header as valid
     */
    @ConfigItem
    public Optional<String> headers;
    /**
     * HTTP headers exposed in CORS
     *
     * Comma separated list of valid headers. ex: X-Custom,Content-Disposition
     * 
     * default: <empty>
     */
    @ConfigItem
    public Optional<String> exposedHeaders;

    @Override
    public String toString() {
        return origins.orElse("<>") + methods.orElse("<>") + headers.orElse("<>");
    }
}
