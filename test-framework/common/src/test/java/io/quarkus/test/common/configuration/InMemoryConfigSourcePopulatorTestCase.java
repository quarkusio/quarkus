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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import io.quarkus.deployment.QuarkusConfig;

public class InMemoryConfigSourcePopulatorTestCase {

    @Test
    public void testPopulateConfigurationPropertiesFromAnnotations() {

        final InMemoryConfigSourcePopulator inMemoryConfigSourcePopulator = new InMemoryConfigSourcePopulator(
                Thread.currentThread().getContextClassLoader());
        inMemoryConfigSourcePopulator.populate(AnnotationOnlyProperties.class);

        final String greetingPrefix = QuarkusConfig.getString("greeting.prefix", null, false);
        final String greetingMessage = QuarkusConfig.getString("greeting.message", null, false);

        assertEquals("Hello", greetingPrefix);
        assertEquals("World", greetingMessage);
    }

    @Test
    public void testPopulateConfigurationPropertiesFromInitializer() {

        final InMemoryConfigSourcePopulator inMemoryConfigSourcePopulator = new InMemoryConfigSourcePopulator(
                Thread.currentThread().getContextClassLoader());
        inMemoryConfigSourcePopulator.populate(AnnotationOnlyInitilizer.class);

        final String greetingPrefix = QuarkusConfig.getString("greeting.prefix", null, false);

        assertEquals("Good Bye", greetingPrefix);

    }

    @Test
    public void testPopulateConfigurationPropertiesFromInitializerAndAnnotations() {

        final InMemoryConfigSourcePopulator inMemoryConfigSourcePopulator = new InMemoryConfigSourcePopulator(
                Thread.currentThread().getContextClassLoader());
        inMemoryConfigSourcePopulator.populate(AnnotionWithInitializerAndProperties.class);

        final String greetingPrefix = QuarkusConfig.getString("greeting.prefix", null, false);
        final String greetingMessage = QuarkusConfig.getString("greeting.message", null, false);

        assertEquals("Hello", greetingPrefix);
        assertEquals("World", greetingMessage);
    }

    @ConfigurationProperty(key = "greeting.message", value = "World")
    @ConfigurationProperty(key = "greeting.prefix", value = "Hello")
    private static class AnnotationOnlyProperties {
    }

    @TestConfiguration(CustomInitializer.class)
    private static class AnnotationOnlyInitilizer {
    }

    @ConfigurationProperty(key = "greeting.message", value = "World")
    @ConfigurationProperty(key = "greeting.prefix", value = "Hello")
    @TestConfiguration(CustomInitializer.class)
    private static class AnnotionWithInitializerAndProperties {
    }

    private static class CustomInitializer implements TestConfigurationInitializer {

        public CustomInitializer() {
            super();
        }

        @Override
        public Map<String, String> initialize() {
            final Map<String, String> map = new HashMap<>();
            map.put("greeting.prefix", "Good Bye");
            return map;
        }
    }
}
