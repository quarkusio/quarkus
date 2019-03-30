/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.camel.core.runtime;

import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.Registry;

public interface CamelRuntime {

    String PFX_CAMEL = "camel.";
    String PFX_CAMEL_PROPERTIES = PFX_CAMEL + "component.properties.";
    String PFX_CAMEL_CONTEXT = PFX_CAMEL + "context.";

    CamelContext getContext();

    Registry getRegistry();

    CamelConfig.BuildTime getBuildTimeConfig();

    CamelConfig.Runtime getRuntimeConfig();

    void init(CamelConfig.BuildTime buildTimeConfig);

    void start(CamelConfig.Runtime runtimeConfig) throws Exception;

    void stop() throws Exception;

    void addProperties(Properties properties);

    void addProperty(String key, Object value);

}
