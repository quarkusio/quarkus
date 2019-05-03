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

package io.quarkus.deployment.builditem.substrate;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a Service Provider registration.
 * When processed, it embeds the service interface descriptor (META-INF/services/...) and allow reflection (instantiation only)
 * on a set of provider
 * classes.
 */
public final class ServiceProviderBuildItem extends MultiBuildItem {

    public static final String SPI_ROOT = "META-INF/services/";
    private final String serviceInterface;
    private final List<String> providers;

    public ServiceProviderBuildItem(String serviceInterfaceClassName, String... providerClassNames) {
        this(serviceInterfaceClassName, Arrays.asList(providerClassNames));
    }

    public ServiceProviderBuildItem(String serviceInterfaceClassName, List<String> providers) {
        this.serviceInterface = Objects.requireNonNull(serviceInterfaceClassName, "The service interface must not be `null`");
        this.providers = providers;

        // Validation
        if (serviceInterface.length() == 0) {
            throw new IllegalArgumentException("The serviceDescriptorFile interface cannot be blank");
        }

        providers.forEach(s -> {
            if (s == null || s.length() == 0) {
                throw new IllegalArgumentException("The provider class name cannot be null or blank");
            }
        });
    }

    public List<String> providers() {
        return providers;
    }

    public String serviceDescriptorFile() {
        return SPI_ROOT + serviceInterface;
    }
}
