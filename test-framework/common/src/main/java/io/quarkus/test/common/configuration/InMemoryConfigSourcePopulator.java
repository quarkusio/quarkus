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

import io.quarkus.runtime.configuration.ExpandingConfigSource;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Class that populates configuration parameters from test to In Memory Config Source
 */
public final class InMemoryConfigSourcePopulator {

    public void populate(final Class<?> testClass) {

        final InMemoryConfigSource inMemoryConfigSource = findInMemoryConfigSource();
        if (inMemoryConfigSource != null) {
            final ConfigurationPropertiesExtractor configurationPropertiesExtractor = new ConfigurationPropertiesExtractor();
            inMemoryConfigSource.addProperties(configurationPropertiesExtractor.extract(testClass));
        }
    }

    private InMemoryConfigSource findInMemoryConfigSource() {

        final Iterable<ConfigSource> configSources = ConfigProvider.getConfig().getConfigSources();

        for (ConfigSource configSource : configSources) {
            if (InMemoryConfigSource.NAME.equals(configSource.getName())) {

                if (ExpandingConfigSource.class.isAssignableFrom(configSource.getClass())) {
                    final ConfigSource delegate = ((ExpandingConfigSource) configSource).getDelegate();
                    return (InMemoryConfigSource) delegate;
                }

                return (InMemoryConfigSource) configSource;
            }
        }

        return null;
    }

}
