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

import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConfigurationTestPropertiesBuilderTestCase {

    @Test
    public void testValidPropertiesSet() {

        final Map<String, String> configuration = ConfigurationTestPropertiesBuilder.configuration("key1=value1", "key2=value2");

        assertEquals("value1", configuration.get("key1"));
        assertEquals("value2", configuration.get("key2"));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidPropertiesSet() {
        ConfigurationTestPropertiesBuilder.configuration("key1=value1", "key2value2");
    }

    @Test
    public void testNoneValuePropertySet() {

        final Map<String, String> configuration = ConfigurationTestPropertiesBuilder.configuration("key1=");

        assertEquals("", configuration.get("key1"));

    }

}
