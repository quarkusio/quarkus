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

package io.quarkus.test.common.configuration;

import java.util.HashMap;
import java.util.Map;

public class ConfigurationTestPropertiesBuilder {

    /**
     * Method that create a Map from Strings to be used in configuration creation.
     * Separator between key and value is the equal (=) sign.
     * {@code ConfigurationTestPropertiesBuilder.configuration(property1=value1, property2=value2)}
     *
     * @param keyValues
     *        other key values tuples. Notice that each element is of the form property=value
     *
     * @return Map of elements
     */
    public static Map<String, String> configuration(String... keyValues) {

        final Map<String, String> configurationProperties = new HashMap<>();

        for (String keyValue : keyValues) {

            final int separator = keyValue.indexOf('=');

            if (separator < 1) {
                throw new IllegalArgumentException(
                        String.format("Each string must be of form key=value but %s found.", keyValue));
            }

            String value = ((separator + 1) > keyValue.length()) ? "" : keyValue.substring(separator + 1);

            configurationProperties.put(keyValue.substring(0, separator).trim(), value.trim());
        }

        return configurationProperties;
    }
}