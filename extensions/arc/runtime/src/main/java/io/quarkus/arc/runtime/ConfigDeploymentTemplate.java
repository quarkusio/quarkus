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

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.inject.spi.DeploymentException;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.annotations.Template;

/**
 * @author Martin Kouba
 */
@Template
public class ConfigDeploymentTemplate {

    public void validateConfigProperties(Map<String, Set<Class<?>>> properties) {
        Config config = ConfigProvider.getConfig();
        for (Entry<String, Set<Class<?>>> entry : properties.entrySet()) {
            Set<Class<?>> propertyTypes = entry.getValue();
            for (Class<?> propertyType : propertyTypes) {
                // Copy SmallRye logic - for collections, we only check if the property config exists without trying to convert it
                if (Collection.class.isAssignableFrom(propertyType)) {
                    propertyType = String.class;
                }
                if (!config.getOptionalValue(entry.getKey(), propertyType).isPresent()) {
                    throw new DeploymentException(
                            "No config value of type " + entry.getValue() + " exists for: " + entry.getKey());
                }
            }
        }
    }

}
