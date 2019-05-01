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

package io.quarkus.it.main;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.configuration.ConfigurationProperty;
import io.quarkus.test.common.configuration.ConfigurationTestPropertiesBuilder;
import io.quarkus.test.common.configuration.TestConfiguration;
import io.quarkus.test.common.configuration.TestConfigurationInitializer;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@ConfigurationProperty(key = "test.key", value = "value1")
@TestConfiguration(JUnit5CustomConfigurationPropertiesTestCase.CustomProperties.class)
public class JUnit5CustomConfigurationPropertiesTestCase {

    @Inject
    Config config;

    @Test
    public void testCustomPropertyFromAnnotation() {
        assertEquals("value1", config.getValue("test.key", String.class));
    }

    @Test
    public void testCustomPropertiesFroInitializerClass() {
        assertEquals("value1", config.getValue("test.key1", String.class));
        assertEquals("value2", config.getValue("test.key2", String.class));
    }

    public static class CustomProperties implements TestConfigurationInitializer {

        @Override
        public Map<String, String> initilize() {
            return ConfigurationTestPropertiesBuilder
                    .configuration(
                            "test.key1=value1",
                            "test.key2=value2");
        }
    }

}
