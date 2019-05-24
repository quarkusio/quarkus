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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.wildfly.common.annotation.NotNull;

public class ConfigurationPropertiesExtractor {

    /**
     * Gets all configuration properties from annotations.
     * 
     * @param testClass
     * @return Map with properties.
     */
    public @NotNull Map<String, String> extract(final Class<?> testClass) {
        final TestConfiguration testConfiguration = testClass.getAnnotation(TestConfiguration.class);
        final ConfigurationProperty[] configurationProperties = testClass.getAnnotationsByType(ConfigurationProperty.class);
        final Map<String, String> configuration = initialConfigurationProperties(testConfiguration);
        configuration.putAll(getAnnotatedProperties(configurationProperties));

        return configuration;
    }

    private Map<String, String> getAnnotatedProperties(final ConfigurationProperty[] configurationPropertiesAnnotations) {
        final Map<String, String> configurationProperties = new HashMap<>();

        if (configurationPropertiesAnnotations != null) {
            for (ConfigurationProperty configurationPropertiesAnnotation : configurationPropertiesAnnotations) {
                configurationProperties.put(configurationPropertiesAnnotation.key(),
                        configurationPropertiesAnnotation.value());
            }
        }

        return configurationProperties;
    }

    private Map<String, String> initialConfigurationProperties(final TestConfiguration testConfiguration) {

        final Map<String, String> configurationProperties = new HashMap<>();

        if (testConfiguration != null) {
            final TestConfigurationInitializer testConfigurationInitializer = instantiateTestConfiguration(
                    testConfiguration.value());
            configurationProperties.putAll(testConfigurationInitializer.initialize());
        }

        return configurationProperties;

    }

    private TestConfigurationInitializer instantiateTestConfiguration(
            final Class<? extends TestConfigurationInitializer> testConfigurationInitializerClass) {
        try {
            final Constructor<? extends TestConfigurationInitializer> constructor = testConfigurationInitializerClass
                    .getConstructor();
            return constructor.newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

}
