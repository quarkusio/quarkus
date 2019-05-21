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

package io.quarkus.arc.runtime;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.inject.spi.DeploymentException;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import io.quarkus.runtime.annotations.Template;

/**
 * @author Martin Kouba
 */
@Template
public class ConfigDeploymentTemplate {

    public void validateConfigProperties(Map<String, Set<String>> properties) {
        Config config = ConfigProviderResolver.instance().getConfig();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            ConfigDeploymentTemplate.class.getClassLoader();
        }
        for (Entry<String, Set<String>> entry : properties.entrySet()) {
            Set<String> propertyTypes = entry.getValue();
            for (String propertyType : propertyTypes) {
                Class<?> propertyClass = load(propertyType, cl);
                // For parameterized types and arrays, we only check if the property config exists without trying to convert it
                if (propertyClass.isArray() || propertyClass.getTypeParameters().length > 0) {
                    propertyClass = String.class;
                }
                if (!config.getOptionalValue(entry.getKey(), propertyClass).isPresent()) {
                    throw new DeploymentException(
                            "No config value of type " + entry.getValue() + " exists for: " + entry.getKey());
                }
            }
        }
    }

    private Class<?> load(String className, ClassLoader cl) {
        switch (className) {
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "char":
                return char.class;
            case "void":
                return void.class;
        }
        try {
            return Class.forName(className, true, cl);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load the config property type: " + className);
        }
    }

}
