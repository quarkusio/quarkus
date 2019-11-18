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
package io.quarkus.config;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Turns a {@link Config} into a JSON object with all config sources and properties as JSON. The config sources are
 * sorted descending by ordinal, the properties by name. If no config is defined an empty JSON object is returned.
 *
 * <p>
 * A typical output might look like:
 * </p>
 *
 * <pre>
 * {
 *   "sources": [
 *     {
 *       "source": "source0",
 *       "ordinal": 200,
 *       "properties": {
 *         "key": "value"
 *       }
 *     },
 *     {
 *       "source": "source1",
 *       "ordinal": 100,
 *       "properties": {
 *         "key": "value"
 *       }
 *     }
 *   ]
 * }
 * </pre>
 */
class ConfigDumper {

    private static final Logger log = Logger.getLogger("io.quarkus.config");

    JsonObject dump(Config config, JsonBuilderFactory factory) {
        if (config != null) {
            if (config.getConfigSources().iterator().hasNext()) {
                JsonObjectBuilder jsonRoot = factory.createObjectBuilder();
                JsonArrayBuilder jsonSources = factory.createArrayBuilder();
                for (ConfigSource source : config.getConfigSources()) {
                    JsonObjectBuilder jsonSource = factory.createObjectBuilder();
                    jsonSource.add("source", source.getName())
                            .add("ordinal", source.getOrdinal());
                    Set<String> propertyNames = source.getPropertyNames();
                    if (!propertyNames.isEmpty()) {
                        SortedSet<String> sortedPropertyNames = new TreeSet<>(propertyNames);
                        JsonObjectBuilder jsonProperties = factory.createObjectBuilder();
                        for (String propertyName : sortedPropertyNames) {
                            try {
                                jsonProperties.add(propertyName, source.getValue(propertyName));
                            } catch (Throwable t) {
                                log.severe("Cannot get configuration value for '" + propertyName + "': " +
                                        t.getMessage());
                            }
                        }
                        jsonSource.add("properties", jsonProperties);
                    }
                    jsonSources.add(jsonSource);
                }
                jsonRoot.add("sources", jsonSources);
                return jsonRoot.build();
            } else {
                return JsonObject.EMPTY_JSON_OBJECT;
            }
        } else {
            return JsonObject.EMPTY_JSON_OBJECT;
        }
    }
}
